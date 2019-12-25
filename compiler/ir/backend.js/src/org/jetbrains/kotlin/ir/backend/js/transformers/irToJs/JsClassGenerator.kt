/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
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

    fun generate(): JsStatement {
        assert(!irClass.descriptor.isExpect)

        val transformer = IrDeclarationToJsTransformer()

        // Properties might be lowered out of classes
        // We'll use IrSimpleFunction::correspondingProperty to collect them into set
        val properties = mutableSetOf<IrProperty>()

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrConstructor -> {
                    classBlock.statements += declaration.accept(transformer, context)
                    classModel.preDeclarationBlock.statements += generateInheritanceCode()
                }
                is IrSimpleFunction -> {
                    properties.addIfNotNull(declaration.correspondingPropertySymbol?.owner)
                    generateMemberFunction(declaration)?.let { classBlock.statements += it }
                }
                is IrClass -> {
                    classBlock.statements += JsClassGenerator(declaration, context).generate()
                }
                is IrField -> {
                }
                else -> {
                    error("Unexpected declaration in class: ${declaration.descriptor}")
                }
            }
        }

        classBlock.statements += generateClassMetadata()

        if (!irClass.isInterface && !irClass.isEnumClass && !irClass.isEnumEntry) {
            for (property in properties) {
                if (property.getter?.extensionReceiverParameter != null || property.setter?.extensionReceiverParameter != null)
                    continue

                if (property.visibility != Visibilities.PUBLIC)
                    continue

                if (property.origin == IrDeclarationOrigin.FAKE_OVERRIDE)
                    continue

                fun IrSimpleFunction.accessorRef(): JsNameRef? =
                    when (visibility) {
                        Visibilities.PRIVATE -> null
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

    private fun generateMemberFunction(declaration: IrSimpleFunction): JsStatement? {

        val translatedFunction = declaration.run { if (isReal) accept(IrFunctionToJsTransformer(), context) else null }
        assert(!declaration.isStaticMethodOfClass)

        val memberName = context.getNameForMemberFunction(declaration.realOverrideTarget)
        val memberRef = JsNameRef(memberName, classPrototypeRef)

        translatedFunction?.let { return jsAssignment(memberRef, it.apply { name = null }).makeStmt() }

        // do not generate code like
        // interface I { foo() = "OK" }
        // interface II : I
        // II.prototype.foo = I.prototype.foo
        if (!irClass.isInterface) {
            declaration.realOverrideTarget.let { it ->
                val implClassDeclaration = it.parent as IrClass

                if (!implClassDeclaration.defaultType.isAny() && !it.isEffectivelyExternal()) {
                    val implMethodName = context.getNameForMemberFunction(it)
                    val implClassName = context.getNameForClass(implClassDeclaration)

                    val implClassPrototype = prototypeOf(implClassName.makeRef())
                    val implMemberRef = JsNameRef(implMethodName, implClassPrototype)

                    classModel.postDeclarationBlock.statements += jsAssignment(memberRef, implMemberRef).makeStmt()
                }
            }
        }

        return null
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
        val functionTypeOrSubtype = irClass.defaultType.isFunctionTypeOrSubtype()
        return JsPropertyInitializer(
            JsNameRef(Namer.METADATA_INTERFACES),
            JsArrayLiteral(
                irClass.superTypes.mapNotNull {
                    val symbol = it.classifierOrFail as IrClassSymbol
                    // TODO: make sure that there is a test which breaks when isExternal is used here instead of isEffectivelyExternal
                    if (symbol.isInterface && !functionTypeOrSubtype && !symbol.isEffectivelyExternal) {
                        JsNameRef(context.getNameForClass(symbol.owner))
                    } else null
                }
            )
        )
    }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true
private val IrClassifierSymbol.isEffectivelyExternal get() = (owner as? IrDeclaration)?.isEffectivelyExternal() == true

class JsIrClassModel(val klass: IrClass) {
    val superClasses = klass.superTypes.map { it.classifierOrFail as IrClassSymbol }

    val preDeclarationBlock = JsGlobalBlock()
    val postDeclarationBlock = JsGlobalBlock()
}
