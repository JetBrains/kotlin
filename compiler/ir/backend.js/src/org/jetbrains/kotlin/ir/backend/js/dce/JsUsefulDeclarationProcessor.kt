/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.JsSymbols.RuntimeMetadataKind
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.isBuiltInClass
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*

internal class JsUsefulDeclarationProcessor(
    override val context: JsIrBackendContext,
    printReachabilityInfo: Boolean,
    removeUnusedAssociatedObjects: Boolean
) : UsefulDeclarationProcessor(printReachabilityInfo, removeUnusedAssociatedObjects) {
    private val equalsMethod = getMethodOfAny("equals")
    private val hashCodeMethod = getMethodOfAny("hashCode")

    override val bodyVisitor: BodyVisitorBase = object : BodyVisitorBase() {
        override fun visitCall(expression: IrCall, data: IrDeclaration) {
            super.visitCall(expression, data)

            if (tryToProcessIntrinsicCall(expression, data)) return

            expression.symbol.owner
                .takeIf { it.isFakeOverride && it.parentAsClass.isInterface }
                ?.let { it.realOverrideTarget.enqueue(data, "overridden method call") }
        }

        private fun tryToProcessIntrinsicCall(expression: IrCall, data: IrDeclaration): Boolean {
            if (expression.usePrototype(data)) {
                context.symbols.jsPrototypeOfSymbol.owner.enqueue(expression.symbol.owner, "access to super type")
            }

            return when (expression.symbol) {
                context.symbols.jsBoxIntrinsic -> {
                    val inlineClass = expression.typeArguments[0]?.let {
                        context.inlineClassesUtils.getRuntimeClassFor(it)
                    } ?: compilationException("Unexpected type argument in box intrinsic", expression)

                    val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }
                    constructor.enqueue(data, "intrinsic: jsBoxIntrinsic")
                    true
                }

                context.symbols.jsClass -> {
                    val ref = expression.typeArguments[0]!!.classifierOrFail.owner as IrDeclaration
                    ref.enqueue(data, "intrinsic: jsClass")
                    referencedJsClasses += ref
                    // When class reference provided as parameter to external function
                    // It can be instantiated by external JS script
                    // Need to leave constructor for this
                    // https://youtrack.jetbrains.com/issue/KT-46672
                    // TODO: Possibly solution with origin is not so good
                    //  There is option with applying this hack to jsGetKClass
                    if (expression.origin == JsStatementOrigins.CLASS_REFERENCE) {
                        // Maybe we need to filter primary constructor
                        // Although at this time, we should have only primary constructor
                        (ref as IrClass)
                            .constructors
                            .forEach {
                                it.enqueue(data, "intrinsic: jsClass (constructor)")
                            }
                    }
                    true
                }

                context.reflectionSymbols.getKClassFromExpression -> {
                    val ref = expression.typeArguments[0]?.classOrNull ?: context.irBuiltIns.anyClass
                    referencedJsClassesFromExpressions += ref.owner
                    true
                }

                context.reflectionSymbols.getKClass -> {
                    expression.typeArguments[0]?.classOrNull?.owner?.let(::addConstructedClass)
                    true
                }

                context.symbols.jsObjectCreateSymbol -> {
                    val classToCreate = expression.typeArguments[0]!!.classifierOrFail.owner as IrClass
                    classToCreate.enqueue(data, "intrinsic: jsObjectCreateSymbol")
                    addConstructedClass(classToCreate)
                    true
                }

                context.symbols.jsCreateThisSymbol -> {
                    val jsClassOrThis = expression.arguments[0]

                    val classTypeToCreate = when (jsClassOrThis) {
                        is IrCall -> jsClassOrThis.typeArguments[0]!!
                        is IrGetValue -> jsClassOrThis.type
                        else -> irError("Unexpected first argument of createThis function call") {
                            jsClassOrThis?.let { withIrEntry("jsClassOrThis", it) }
                        }
                    }

                    val classToCreate = classTypeToCreate.classifierOrFail.owner as IrClass

                    classToCreate.enqueue(data, "intrinsic: jsCreateThis")
                    addConstructedClass(classToCreate)
                    true
                }

                context.symbols.jsEquals -> {
                    equalsMethod.enqueue(data, "intrinsic: jsEquals")
                    true
                }

                context.symbols.jsToString -> {
                    toStringMethod.enqueue(data, "intrinsic: jsToString")
                    true
                }

                context.symbols.jsHashCode -> {
                    hashCodeMethod.enqueue(data, "intrinsic: jsHashCode")
                    true
                }

                context.symbols.jsPlus -> {
                    if (expression.arguments[0]?.type?.classOrNull == context.irBuiltIns.stringClass) {
                        toStringMethod.enqueue(data, "intrinsic: jsPlus")
                    }
                    true
                }

                context.symbols.jsInvokeSuspendSuperType,
                context.symbols.jsInvokeSuspendSuperTypeWithReceiver,
                context.symbols.jsInvokeSuspendSuperTypeWithReceiverAndParam -> {
                    invokeFunForLambda(expression)
                        .enqueue(data, "intrinsic: suspendSuperType")
                    true
                }
                else -> false
            }
        }
    }

    override fun addConstructedClass(irClass: IrClass) {
        super.addConstructedClass(irClass)

        if (irClass.isClass) {
            irClass.findDefaultConstructorForReflection()?.enqueue(irClass, "intrinsic: KClass<*>.createInstance")
        }
    }

    override fun processSuperTypes(irClass: IrClass) {
        irClass.superTypes.forEach {
            val shouldClassBeKept = it.classOrNull?.let { context.keeper.shouldKeep(it.owner) } ?: false
            if (!it.isInterface() || shouldClassBeKept) {
                (it.classifierOrNull as? IrClassSymbol)?.owner?.enqueue(irClass, "superTypes")
            }
        }
    }

    override fun processClass(irClass: IrClass) {
        super.processClass(irClass)

        if (context.keeper.shouldKeep(irClass)) {
            irClass.declarations
                .filter { context.keeper.shouldKeep(it) }
                .forEach { declaration ->
                    declaration.enqueue(irClass, "kept declaration")
                }
        }

        if (!irClass.containsMetadata()) return

        val metadataKinds = when {
            irClass.isObject -> listOf(RuntimeMetadataKind.COMPANION_OBJECT, RuntimeMetadataKind.OBJECT)
            irClass.isInterface -> listOf(RuntimeMetadataKind.INTERFACE)
            else -> listOf(
                RuntimeMetadataKind.CLASS,
                RuntimeMetadataKind.LAMBDA,
                RuntimeMetadataKind.COROUTINE,
                RuntimeMetadataKind.FUNCTION_REFERENCE,
            )
        }

        for (metadataKind in metadataKinds) {
            context.symbols
                .getInitMetadataSymbol(metadataKind)
                ?.owner
                ?.enqueue(irClass, "${metadataKind.name.lowercase().replace('_', ' ')} metadata")
        }

        if (irClass.containsInterfaceDefaultImplementation()) {
            context.symbols.jsPrototypeOfSymbol.owner.enqueue(irClass, "interface default implementation")
        }

        if (irClass.isInner || irClass.isObject) {
            context.symbols.jsDefinePropertySymbol.owner.enqueue(irClass, "object lazy initialization")
        }

        if (context.es6mode) return

        if (!irClass.isInterface) {
            context.symbols.jsPrototypeOfSymbol.owner.enqueue(irClass, "class prototype access")
        }

        if (irClass.superTypes.any { !it.isInterface() }) {
            context.symbols.jsObjectCreateSymbol.owner.enqueue(irClass, "class inheritance code")
        }
    }

    override fun processSimpleFunction(irFunction: IrSimpleFunction) {
        super.processSimpleFunction(irFunction)

        if (irFunction.isEs6ConstructorReplacement) {
            addConstructedClass(irFunction.dispatchReceiverParameter?.type?.classOrNull?.owner!!)
        }

        if (irFunction.isReal && irFunction.body != null) {
            irFunction.parentClassOrNull?.takeIf { it.isInterface }?.enqueue(irFunction, "interface default method is used")
        }

        val property = irFunction.correspondingPropertySymbol?.owner ?: return

        if (property.isExported(context) || property.getJsName() != null || property.isOverriddenExternal()) {
            context.symbols.jsPrototypeOfSymbol.owner.enqueue(irFunction, "property for export")
            context.symbols.jsDefinePropertySymbol.owner.enqueue(irFunction, "property for export")
        }
    }

    private fun IrClass.containsMetadata(): Boolean =
        !isExternal && !isExpect && !isBuiltInClass(this)

    override fun processConstructedClassDeclaration(declaration: IrDeclaration) {
        if (declaration.isReachable()) return

        super.processConstructedClassDeclaration(declaration)

        if (declaration is IrSimpleFunction && declaration.getJsNameOrKotlinName().asString() == "valueOf") {
            declaration.enqueue(declaration, "valueOf")
        }

        // A hack to support `toJson` and other js-specific members
        if (declaration.getJsName() != null ||
            declaration is IrField && declaration.correspondingPropertySymbol?.owner?.getJsName() != null ||
            declaration is IrSimpleFunction && declaration.correspondingPropertySymbol?.owner?.getJsName() != null
        ) {
            declaration.enqueue(declaration, "annotated by @JsName")
        }
    }

    private val referencedJsClasses = hashSetOf<IrDeclaration>()
    private val referencedJsClassesFromExpressions = hashSetOf<IrClass>()

    override fun handleAssociatedObjects() {
        //Handle objects, constructed via `findAssociatedObject` annotation
        referencedJsClassesFromExpressions += constructedClasses.filterDescendantsOf(referencedJsClassesFromExpressions) // Grow the set of possible results of instance::class expression
        for (klass in classesWithObjectAssociations) {
            if (removeUnusedAssociatedObjects && klass !in referencedJsClasses && klass !in referencedJsClassesFromExpressions) continue

            for (annotation in klass.annotations) {
                val annotationClass = annotation.symbol.owner.constructedClass
                if (removeUnusedAssociatedObjects && annotationClass !in referencedJsClasses) continue

                annotation.associatedObject()?.objectGetInstanceFunction?.enqueue(klass, "associated object factory")
            }
        }
    }

    override fun isExported(declaration: IrDeclaration): Boolean = declaration.isExported(context)

    private fun IrCall.usePrototype(container: IrDeclaration?): Boolean {
        if (superQualifierSymbol == null) return false

        val currentFun = (container as? IrSimpleFunction)
        val currentClass = currentFun?.parentClassOrNull

        return !context.es6mode ||
                currentFun?.dispatchReceiverParameter == null ||
                currentClass != null && (currentClass.isInner || currentClass.isOriginallyLocal)
    }

    private fun IrClass.containsInterfaceDefaultImplementation(): Boolean {
        return superTypes.any { it.classOrNull?.owner?.isExternal == true } ||
                isExported(context) ||
                isInterface && declarations.any { it is IrFunction && it.body != null }
    }
}

private fun Collection<IrClass>.filterDescendantsOf(bases: Collection<IrClass>): Collection<IrClass> {
    val visited = hashSetOf<IrClass>()
    val baseDescendants = hashSetOf<IrClass>()
    baseDescendants += bases

    fun overridesAnyBase(klass: IrClass): Boolean {
        if (klass in baseDescendants) return true
        if (klass in visited) return false

        visited += klass

        klass.superTypes.forEach {
            (it.classifierOrNull as? IrClassSymbol)?.owner?.let { klass ->
                if (overridesAnyBase(klass)) {
                    baseDescendants += klass
                    return true
                }
            }
        }

        return false
    }

    return this.filter { overridesAnyBase(it) }
}
