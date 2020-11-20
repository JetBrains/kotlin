/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.utils.addIfNotNull

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {

    private val className = context.getNameForClass(irClass)
    private val classNameRef = className.makeRef()
    private val baseClass: IrType? = irClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }
    private val baseClassName = baseClass?.let {
        context.getNameForClass(baseClass.classifierOrFail.owner as IrClass)
    }
    private val classPrototypeRef = prototypeOf(classNameRef)
    private val classBlock = JsGlobalBlock()
    private val classModel = JsIrClassModel(irClass)

    private val es6mode = context.staticContext.backendContext.es6mode

    fun generate(): JsStatement {
        assert(!irClass.isExpect)

        if (!es6mode) maybeGeneratePrimaryConstructor()
        val transformer = IrDeclarationToJsTransformer()

        // Properties might be lowered out of classes
        // We'll use IrSimpleFunction::correspondingProperty to collect them into set
        val properties = mutableSetOf<IrProperty>()

        val jsClass = JsClass(name = className)

        if (baseClass != null && !baseClass.isAny()) {
            jsClass.baseClass = baseClassName?.makeRef()
        }

        if (es6mode) classModel.preDeclarationBlock.statements += jsClass.makeStmt()

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrConstructor -> {
                    if (es6mode) {
                        declaration.accept(IrFunctionToJsTransformer(), context).let {
                            //HACK: add superCall to Error
                            if ((baseClass?.classifierOrNull?.owner as? IrClass)?.symbol === context.staticContext.backendContext.throwableClass) {
                                it.body.statements.add(0, JsInvocation(JsNameRef("super")).makeStmt())
                            }

                            if (it.body.statements.any { it !is JsEmpty }) {
                                jsClass.constructor = it
                            }
                        }
                    } else {
                        classBlock.statements += declaration.accept(transformer, context)
                        classModel.preDeclarationBlock.statements += generateInheritanceCode()
                    }
                }
                is IrSimpleFunction -> {
                    properties.addIfNotNull(declaration.correspondingPropertySymbol?.owner)

                    if (es6mode) {
                        val (_, function) = generateMemberFunction(declaration)
                        function?.let { jsClass.members += it }
                    } else {
                        val (memberRef, function) = generateMemberFunction(declaration)
                        function?.let { classBlock.statements += jsAssignment(memberRef, it.apply { name = null }).makeStmt() }
                    }
                }
                is IrClass -> {
                    classBlock.statements += JsClassGenerator(declaration, context).generate()
                }
                is IrField -> {
                }
                else -> {
                    error("Unexpected declaration in class: ${declaration.render()}")
                }
            }
        }

        classBlock.statements += generateClassMetadata()

        if (!irClass.isInterface && !irClass.isEnumClass && !irClass.isEnumEntry) {
            for (property in properties) {
                if (property.getter?.extensionReceiverParameter != null || property.setter?.extensionReceiverParameter != null)
                    continue

                if (property.visibility != DescriptorVisibilities.PUBLIC)
                    continue

                if (property.isFakeOverride)
                    continue

                fun IrSimpleFunction.accessorRef(): JsNameRef? =
                    when (visibility) {
                        DescriptorVisibilities.PRIVATE -> null
                        else -> JsNameRef(
                            context.getNameForMemberFunction(this),
                            classPrototypeRef
                        )
                    }

                if (irClass.isExported(context.staticContext.backendContext) ||
                    property.getter?.overridesExternal() == true ||
                    property.getJsName() != null
                ) {
                    val getterRef = property.getter?.accessorRef()
                    val setterRef = property.setter?.accessorRef()
                    classBlock.statements += JsExpressionStatement(
                        defineProperty(
                            classPrototypeRef,
                            context.getNameForProperty(property).ident,
                            getter = getterRef,
                            setter = setterRef
                        )
                    )
                }
            }
        }
        context.staticContext.classModels[irClass.symbol] = classModel
        return classBlock
    }

    private fun IrSimpleFunction.overridesExternal(): Boolean {
        if (this.isEffectivelyExternal()) return true

        return this.overriddenSymbols.any { it.owner.overridesExternal() }
    }

    private fun IrClass.shouldCopyFrom(): Boolean {
        return isInterface && !isEffectivelyExternal()
    }

    private fun generateMemberFunction(declaration: IrSimpleFunction): Pair<JsNameRef, JsFunction?> {
        val memberName = context.getNameForMemberFunction(declaration.realOverrideTarget)
        val memberRef = JsNameRef(memberName, classPrototypeRef)

        if (declaration.isReal && declaration.body != null) {
            val translatedFunction = declaration.accept(IrFunctionToJsTransformer(), context)
            assert(!declaration.isStaticMethodOfClass)

            return Pair(memberRef, translatedFunction)
        }

        // do not generate code like
        // interface I { foo() = "OK" }
        // interface II : I
        // II.prototype.foo = I.prototype.foo
        if (!irClass.isInterface) {
            declaration.realOverrideTarget.let {
                val implClassDeclaration = it.parent as IrClass

                if (implClassDeclaration.shouldCopyFrom()) {
                    val implMethodName = context.getNameForMemberFunction(it)
                    val implClassName = context.getNameForClass(implClassDeclaration)

                    val implClassPrototype = prototypeOf(implClassName.makeRef())
                    val implMemberRef = JsNameRef(implMethodName, implClassPrototype)

                    classModel.postDeclarationBlock.statements += jsAssignment(memberRef, implMemberRef).makeStmt()
                }
            }
        }

        return Pair(memberRef, null)
    }

    private fun maybeGeneratePrimaryConstructor() {
        if (!irClass.declarations.any { it is IrConstructor }) {
            val func = JsFunction(emptyScope, JsBlock(), "Ctor for ${irClass.name}")
            func.name = className
            classBlock.statements += func.makeStmt()
            classModel.preDeclarationBlock.statements += generateInheritanceCode()
        }
    }

    private fun generateInheritanceCode(): List<JsStatement> {
        if (baseClass == null || baseClass.isAny()) {
            return emptyList()
        }

        val createCall = jsAssignment(
            classPrototypeRef, JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototypeOf(baseClassName!!.makeRef()))
        ).makeStmt()

        val ctorAssign = jsAssignment(JsNameRef(Namer.CONSTRUCTOR_NAME, classPrototypeRef), classNameRef).makeStmt()

        return listOf(createCall, ctorAssign)
    }

    private fun generateClassMetadata(): JsStatement {
        val metadataLiteral = JsObjectLiteral(true)
        val simpleName = irClass.name

        if (!simpleName.isSpecial) {
            val simpleNameProp = JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), JsStringLiteral(simpleName.identifier))
            metadataLiteral.propertyInitializers += simpleNameProp
        }

        val classKind = JsStringLiteral(
            when {
                irClass.isInterface -> "interface"
                irClass.isObject -> "object"
                else -> "class"
            }
        )
        metadataLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_CLASS_KIND), classKind)

        metadataLiteral.propertyInitializers += generateSuperClasses()

        metadataLiteral.propertyInitializers += generateAssociatedKeyProperties()

        if (isCoroutineClass()) {
            metadataLiteral.propertyInitializers += generateSuspendArity()
        }

        return jsAssignment(JsNameRef(Namer.METADATA, classNameRef), metadataLiteral).makeStmt()
    }

    private fun isCoroutineClass(): Boolean = irClass.superTypes.any { it.isSuspendFunctionTypeOrSubtype() }

    private fun generateSuspendArity(): JsPropertyInitializer {
        val arity = irClass.declarations.filterIsInstance<IrSimpleFunction>().first { it.isSuspend }.valueParameters.size
        return JsPropertyInitializer(JsNameRef(Namer.METADATA_SUSPEND_ARITY), JsIntLiteral(arity))
    }

    private fun generateSuperClasses(): JsPropertyInitializer {
        return JsPropertyInitializer(
            JsNameRef(Namer.METADATA_INTERFACES),
            JsArrayLiteral(
                irClass.superTypes.mapNotNull {
                    val symbol = it.classifierOrFail as IrClassSymbol
                    val isFunctionType = it.run { isFunctionOrKFunction() || isSuspendFunctionOrKFunction() }
                    // TODO: make sure that there is a test which breaks when isExternal is used here instead of isEffectivelyExternal
                    if (symbol.isInterface && !isFunctionType && !symbol.isEffectivelyExternal) {
                        JsNameRef(context.getNameForClass(symbol.owner))
                    } else null
                }
            )
        )
    }

    private fun generateAssociatedKeyProperties(): List<JsPropertyInitializer> {
        var result = emptyList<JsPropertyInitializer>()

        context.getAssociatedObjectKey(irClass)?.let { key ->
            result = result + JsPropertyInitializer(JsStringLiteral("associatedObjectKey"), JsIntLiteral(key))
        }

        val associatedObjects = irClass.annotations.mapNotNull { annotation ->
            val annotationClass = annotation.symbol.owner.constructedClass
            context.getAssociatedObjectKey(annotationClass)?.let { key ->
                annotation.associatedObject()?.let { obj ->
                    context.staticContext.backendContext.mapping.objectToGetInstanceFunction[obj]?.let { factory ->
                        JsPropertyInitializer(JsIntLiteral(key), context.staticContext.getNameForStaticFunction(factory).makeRef())
                    }
                }
            }
        }

        if (associatedObjects.isNotEmpty()) {
            result = result + JsPropertyInitializer(JsStringLiteral("associatedObjects"), JsObjectLiteral(associatedObjects))
        }

        return result
    }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true
private val IrClassifierSymbol.isEffectivelyExternal get() = (owner as? IrDeclaration)?.isEffectivelyExternal() == true

class JsIrClassModel(val klass: IrClass) {
    val superClasses = klass.superTypes.map { it.classifierOrFail as IrClassSymbol }

    val preDeclarationBlock = JsGlobalBlock()
    val postDeclarationBlock = JsGlobalBlock()
}
