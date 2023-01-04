/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.isBuiltInClass
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
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
            when (expression.symbol) {
                context.intrinsics.jsBoxIntrinsic -> {
                    val inlineClass = context.inlineClassesUtils.getInlinedClass(expression.getTypeArgument(0)!!)!!
                    val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }
                    constructor.enqueue(data, "intrinsic: jsBoxIntrinsic")
                }

                context.intrinsics.jsClass -> {
                    val ref = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrDeclaration
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
                }

                context.reflectionSymbols.getKClassFromExpression -> {
                    val ref = expression.getTypeArgument(0)?.classOrNull ?: context.irBuiltIns.anyClass
                    referencedJsClassesFromExpressions += ref.owner
                }

                context.intrinsics.jsObjectCreateSymbol -> {
                    val classToCreate = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrClass
                    classToCreate.enqueue(data, "intrinsic: jsObjectCreateSymbol")
                    constructedClasses += classToCreate
                }

                context.intrinsics.jsEquals -> {
                    equalsMethod.enqueue(data, "intrinsic: jsEquals")
                }

                context.intrinsics.jsToString -> {
                    toStringMethod.enqueue(data, "intrinsic: jsToString")
                }

                context.intrinsics.jsHashCode -> {
                    hashCodeMethod.enqueue(data, "intrinsic: jsHashCode")
                }

                context.intrinsics.jsPlus -> {
                    if (expression.getValueArgument(0)?.type?.classOrNull == context.irBuiltIns.stringClass) {
                        toStringMethod.enqueue(data, "intrinsic: jsPlus")
                    }
                }

                context.intrinsics.jsConstruct -> {
                    val callType = expression.getTypeArgument(0)!!
                    val constructor = callType.getClass()!!.primaryConstructor
                    constructor!!.enqueue(data, "ctor call from jsConstruct-intrinsic")
                }

                context.intrinsics.es6DefaultType -> {
                    //same as jsClass
                    val ref = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrDeclaration
                    ref.enqueue(data, "intrinsic: jsClass")
                    referencedJsClasses += ref

                    //Generate klass in `val currResultType = resultType || klass`
                    val arg = expression.getTypeArgument(0)!!
                    val klass = arg.getClass()
                    if (klass != null) {
                        constructedClasses.add(klass)
                    }
                }

                context.intrinsics.jsInvokeSuspendSuperType,
                context.intrinsics.jsInvokeSuspendSuperTypeWithReceiver,
                context.intrinsics.jsInvokeSuspendSuperTypeWithReceiverAndParam -> {
                    invokeFunForLambda(expression)
                        .enqueue(data, "intrinsic: suspendSuperType")
                }
            }
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

        if (irClass.containsMetadata()) {
            when {
                irClass.isObject -> context.intrinsics.metadataObjectConstructorSymbol.owner.enqueue(irClass, "object metadata")

                irClass.isInterface -> {
                    context.intrinsics.implementSymbol.owner.enqueue(irClass, "interface metadata")
                    context.intrinsics.metadataInterfaceConstructorSymbol.owner.enqueue(irClass, "interface metadata")
                }

                else -> {
                    context.intrinsics.metadataClassConstructorSymbol.owner.enqueue(irClass, "class metadata")
                }
            }
        }

        if (!irClass.isExpect && !irClass.isExternal && !irClass.defaultType.isAny()) {
            if (!irClass.isInterface) {
                context.intrinsics.jsPrototypeOfSymbol.owner.enqueue(irClass, "class metadata")
            }

            if (irClass.superTypes.any { !it.isInterface() }) {
                context.intrinsics.jsObjectCreateSymbol.owner.enqueue(irClass, "class metadata")
            }

            if (irClass.isInner || irClass.isObject) {
                context.intrinsics.jsDefinePropertySymbol.owner.enqueue(irClass, "class metadata")
            }

            context.intrinsics.setMetadataForSymbol.owner.enqueue(irClass, "metadata")
        }
    }

    override fun processSimpleFunction(irFunction: IrSimpleFunction) {
        super.processSimpleFunction(irFunction)

        if (irFunction.isReal && irFunction.body != null) {
            irFunction.parentClassOrNull?.takeIf { it.isInterface }?.enqueue(irFunction, "interface default method is used")
        }

        val property = irFunction.correspondingPropertySymbol?.owner ?: return

        if (property.isExported(context) || property.isOverriddenExternal()) {
            context.intrinsics.jsDefinePropertySymbol.owner.enqueue(irFunction, "property for export")
        }
    }

    private fun IrClass.containsMetadata(): Boolean =
        !isExternal && !isExpect && !isBuiltInClass(this)

    override fun processConstructedClassDeclaration(declaration: IrDeclaration) {
        if (declaration in result) return

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

                annotation.associatedObject()?.let { obj ->
                    context.mapping.objectToGetInstanceFunction[obj]?.enqueue(klass, "associated object factory")
                }
            }
        }
    }

    override fun isExported(declaration: IrDeclaration): Boolean = declaration.isExported(context)
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
