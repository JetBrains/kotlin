/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.export.isAllowedFakeOverriddenDeclaration
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.export.isOverriddenExported
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.utils.addIfNotNull

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {

    private val className = context.getNameForClass(irClass)
    private val classNameRef = className.makeRef()
    private val baseClass: IrType? = irClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }

    private val baseClassRef by lazy { // Lazy in case was not collected by namer during JsClassGenerator construction
        if (baseClass != null && !baseClass.isAny()) baseClass.getClassRef(context) else null
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

        val jsClass = JsClass(name = className, baseClass = baseClassRef)

        if (baseClass != null && !baseClass.isAny()) {
            jsClass.baseClass = baseClassRef
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
                        val (memberRef, function) = generateMemberFunction(declaration)
                        function?.let { jsClass.members += it }
                        declaration.generateAssignmentIfMangled(memberRef)
                    } else {
                        val (memberRef, function) = generateMemberFunction(declaration)
                        function?.let { classBlock.statements += jsAssignment(memberRef, it.apply { name = null }).makeStmt() }
                        declaration.generateAssignmentIfMangled(memberRef)
                    }
                }
                is IrClass -> {
//                    classBlock.statements += JsClassGenerator(declaration, context).generate()
                }
                is IrField -> {
                }
                else -> {
                    compilationException(
                        "Unexpected declaration in class",
                        declaration
                    )
                }
            }
        }

        classBlock.statements += generateClassMetadata()

        if (!irClass.isInterface) {
            for (property in properties) {
                if (property.getter?.extensionReceiverParameter != null || property.setter?.extensionReceiverParameter != null)
                    continue

                if (!property.visibility.isPublicAPI)
                    continue

                if (
                    property.isFakeOverride &&
                    !property.isAllowedFakeOverriddenDeclaration(context.staticContext.backendContext)
                )
                    continue

                fun IrSimpleFunction.propertyAccessorForwarder(
                    description: String,
                    callActualAccessor: (JsNameRef) -> JsStatement
                ): JsFunction? =
                    when (visibility) {
                        DescriptorVisibilities.PRIVATE -> null
                        else -> JsFunction(
                            emptyScope,
                            JsBlock(callActualAccessor(JsNameRef(context.getNameForMemberFunction(this), JsThisRef()))),
                            description
                        )
                    }

                val overriddenSymbols = property.getter?.overriddenSymbols.orEmpty()

                // Don't generate `defineProperty` if the property overrides a property from an exported class,
                // because we've already generated `defineProperty` for the base class property.
                // In other words, we only want to generate `defineProperty` once for each property.
                // The exception is case when we override val with var,
                // so we need regenerate `defineProperty` with setter.
                // P.S. If the overridden property is owned by an interface - we should generate defineProperty
                // for overridden property in the first class which override those properties
                val hasOverriddenExportedInterfaceProperties = overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() }
                        && !overriddenSymbols.any { it.owner.parentClassOrNull.isExportedClass() }

                val getterOverridesExternal = property.getter?.overridesExternal() == true
                val overriddenExportedGetter = !property.getter?.overriddenSymbols.isNullOrEmpty() &&
                        property.getter?.isOverriddenExported(context.staticContext.backendContext) == true

                val noOverriddenExportedSetter = property.setter?.isOverriddenExported(context.staticContext.backendContext) == false

                val needsOverride = (overriddenExportedGetter && noOverriddenExportedSetter) ||
                        property.isAllowedFakeOverriddenDeclaration(context.staticContext.backendContext)

                if (irClass.isExported(context.staticContext.backendContext) &&
                    (overriddenSymbols.isEmpty() || needsOverride) ||
                    hasOverriddenExportedInterfaceProperties ||
                    getterOverridesExternal ||
                    property.getJsName() != null
                ) {

                    // Use "direct dispatch" for final properties, i. e. instead of this:
                    //     Object.defineProperty(Foo.prototype, 'prop', {
                    //         configurable: true,
                    //         get: function() { return this._get_prop__0_k$(); },
                    //         set: function(v) { this._set_prop__a4enbm_k$(v); }
                    //     });
                    // emit this:
                    //     Object.defineProperty(Foo.prototype, 'prop', {
                    //         configurable: true,
                    //         get: Foo.prototype._get_prop__0_k$,
                    //         set: Foo.prototype._set_prop__a4enbm_k$
                    //     });

                    val getterForwarder = property.getter
                        .takeIf { it.shouldExportAccessor() }
                        .getOrGenerateIfFinal {
                            propertyAccessorForwarder("getter forwarder") {
                                JsReturn(JsInvocation(it))
                            }
                        }

                    val setterForwarder = property.setter
                        .takeIf { it.shouldExportAccessor() }
                        .getOrGenerateIfFinal {
                            val setterArgName = JsName("value", false)
                            propertyAccessorForwarder("setter forwarder") {
                                JsInvocation(it, JsNameRef(setterArgName)).makeStmt()
                            }?.apply {
                                parameters.add(JsParameter(setterArgName))
                            }
                        }

                    classBlock.statements += JsExpressionStatement(
                        defineProperty(
                            classPrototypeRef,
                            context.getNameForProperty(property).ident,
                            getter = getterForwarder,
                            setter = setterForwarder
                        )
                    )
                }
            }
        }
        context.staticContext.classModels[irClass.symbol] = classModel
        return classBlock
    }

    private inline fun IrSimpleFunction?.getOrGenerateIfFinal(generateFunc: IrSimpleFunction.() -> JsFunction?): JsExpression? {
        if (this == null) return null
        return if (modality == Modality.FINAL) accessorRef() else generateFunc()
    }

    private fun IrSimpleFunction.isDefinedInsideExportedInterface(): Boolean {
        return (!isFakeOverride && parentClassOrNull.isExportedInterface()) ||
                overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() }
    }

    private fun IrSimpleFunction?.shouldExportAccessor(): Boolean {
        if (this == null) return false

        if (parentAsClass.isExported(context.staticContext.backendContext)) return true

        val property = correspondingPropertySymbol!!.owner

        if (property.isOverriddenExported(context.staticContext.backendContext)) {
            return isOverriddenExported(context.staticContext.backendContext)
        }

        return overridesExternal() || property.getJsName() != null
    }

    private fun IrSimpleFunction.accessorRef(): JsNameRef? =
        when (visibility) {
            DescriptorVisibilities.PRIVATE -> null
            else -> JsNameRef(
                context.getNameForMemberFunction(this),
                classPrototypeRef
            )
        }

    private fun IrSimpleFunction.generateAssignmentIfMangled(memberRef: JsExpression) {
        if (
            irClass.isExported(context.staticContext.backendContext) &&
            visibility.isPublicAPI && hasMangledName() &&
            correspondingPropertySymbol == null
        ) {
            classBlock.statements += jsAssignment(prototypeAccessRef(), memberRef).makeStmt()
        }
    }

    private fun IrSimpleFunction.hasMangledName(): Boolean {
        return getJsName() == null && !name.asString().isValidES5Identifier()
    }

    private fun IrSimpleFunction.prototypeAccessRef(): JsExpression {
        return jsElementAccess(name.asString(), classPrototypeRef)
    }

    private fun IrSimpleFunction.overridesExternal(): Boolean {
        if (this.isEffectivelyExternal()) return true

        return this.overriddenSymbols.any { it.owner.overridesExternal() }
    }

    private fun IrClass.shouldCopyFrom(): Boolean {
        return isInterface && !isEffectivelyExternal()
    }

    private fun generateMemberFunction(declaration: IrSimpleFunction): Pair<JsExpression, JsFunction?> {
        val memberName = context.getNameForMemberFunction(declaration.realOverrideTarget)
        val memberRef = jsElementAccess(memberName.ident, classPrototypeRef)

        if (declaration.isReal && declaration.body != null) {
            val translatedFunction: JsFunction = declaration.accept(IrFunctionToJsTransformer(), context)
            assert(!declaration.isStaticMethodOfClass)

            if (irClass.isInterface) {
                classModel.preDeclarationBlock.statements += translatedFunction.makeStmt()
                return Pair(memberRef, null)
            }

            return Pair(memberRef, translatedFunction)
        }

        // do not generate code like
        // interface I { foo() = "OK" }
        // interface II : I
        // II.prototype.foo = I.prototype.foo
        if (!irClass.isInterface) {
            val isFakeOverride = declaration.isFakeOverride
            val missedOverrides = mutableListOf<IrSimpleFunction>()
            declaration.collectRealOverrides()
                .onEach {
                    if (isFakeOverride && it.modality == Modality.ABSTRACT) {
                        missedOverrides.add(it)
                    }
                }
                .find { it.modality != Modality.ABSTRACT }
                ?.let {
                    val implClassDeclaration = it.parent as IrClass

                    if (implClassDeclaration.shouldCopyFrom()) {
                        val reference = context.getNameForStaticDeclaration(it).makeRef()
                        classModel.postDeclarationBlock.statements += jsAssignment(memberRef, reference).makeStmt()
                        if (isFakeOverride) {
                            classModel.postDeclarationBlock.statements += missedOverrides
                                .map { missedOverride ->
                                    val name = context.getNameForMemberFunction(missedOverride)
                                    val ref = jsElementAccess(name.ident, classPrototypeRef)
                                    jsAssignment(ref, reference).makeStmt()
                                }
                        }
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
        val baseClassPrototype = baseClassRef ?: return emptyList()

        val createCall = jsAssignment(
            classPrototypeRef, JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototypeOf(baseClassPrototype))
        ).makeStmt()

        val ctorAssign = jsAssignment(JsNameRef(Namer.CONSTRUCTOR_NAME, classPrototypeRef), classNameRef).makeStmt()

        return listOf(createCall, ctorAssign)
    }

    private fun generateClassMetadata(): JsStatement {
        val metadataConstructor = with(context.staticContext.backendContext.intrinsics) {
            when {
                irClass.isInterface -> metadataInterfaceConstructorSymbol
                irClass.isObject -> metadataObjectConstructorSymbol
                else -> metadataClassConstructorSymbol
            }
        }

        val simpleName = irClass.name
            .takeIf { !it.isSpecial }
            ?.let { JsStringLiteral(it.identifier) }

        val interfaces = generateSuperClasses()
        val associatedObjectKey = generateAssociatedObjectKey()
        val associatedObjects = generateAssociatedObjects()
        val fastPrototype = generateFastPrototype()
        val suspendArity = generateSuspendArity()

        val constructorCall = JsInvocation(
            JsNameRef(context.getNameForStaticFunction(metadataConstructor.owner)),
            listOf(simpleName, interfaces, associatedObjectKey, associatedObjects, suspendArity, fastPrototype)
                .dropLastWhile { it == null }
                .map { it ?: Namer.JS_UNDEFINED }
        )

        return jsAssignment(JsNameRef(Namer.METADATA, classNameRef), constructorCall).makeStmt()
    }

    private fun isCoroutineClass(): Boolean = irClass.superTypes.any { it.isSuspendFunctionTypeOrSubtype() }

    private fun generateSuspendArity(): JsArrayLiteral? {
        if (!isCoroutineClass()) return null

        val arity = context.staticContext.backendContext.mapping.suspendArityStore[irClass]!!
            .map { it.valueParameters.size }
            .distinct()
            .map { JsIntLiteral(it) }

        return JsArrayLiteral(arity)
    }

    private fun generateSuperClasses(): JsArrayLiteral? {
        val parentSymbols = irClass.superTypes.mapNotNull {
            val symbol = it.classifierOrFail as IrClassSymbol
            val isFunctionType = it.isFunctionType()
            // TODO: make sure that there is a test which breaks when isExternal is used here instead of isEffectivelyExternal
            val requireInMetadata = if (context.staticContext.backendContext.baseClassIntoMetadata)
                !it.isAny()
            else
                symbol.isInterface

            if (requireInMetadata && !isFunctionType && !symbol.isEffectivelyExternal) {
                symbol
            } else null
        }

        return parentSymbols
            .takeIf { it.isNotEmpty() }
            ?.run { JsArrayLiteral(map { JsNameRef(context.getNameForClass(it.owner)) }) }
    }

    private fun generateFastPrototype() = baseClassRef?.let { prototypeOf(it) }

    private fun IrType.isFunctionType() = isFunctionOrKFunction() || isSuspendFunctionOrKFunction()

    private fun generateAssociatedObjectKey(): JsIntLiteral? {
        return context.getAssociatedObjectKey(irClass)?.let { JsIntLiteral(it) }
    }

    private fun generateAssociatedObjects(): JsObjectLiteral? {
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

        return associatedObjects
            .takeIf { it.isNotEmpty() }
            ?.let { JsObjectLiteral(it) }
    }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true
private val IrClassifierSymbol.isEffectivelyExternal get() = (owner as? IrDeclaration)?.isEffectivelyExternal() == true

class JsIrClassModel(val klass: IrClass) {
    val superClasses = klass.superTypes.map { it.classifierOrFail as IrClassSymbol }

    val preDeclarationBlock = JsGlobalBlock()
    val postDeclarationBlock = JsGlobalBlock()
}

class JsIrIcClassModel(val superClasses: List<JsName>) {
    val preDeclarationBlock = JsGlobalBlock()
    val postDeclarationBlock = JsGlobalBlock()
}