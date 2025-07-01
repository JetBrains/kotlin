/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.AbstractSuspendFunctionsLowering
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isAllowedFakeOverriddenDeclaration
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.export.isOverriddenEnumProperty
import org.jetbrains.kotlin.ir.backend.js.export.isOverriddenExported
import org.jetbrains.kotlin.backend.common.lower.WebCallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.JsIntrinsics.RuntimeMetadataKind
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.suspendArityStore
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.JsVars.JsVar
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull
import org.jetbrains.kotlin.utils.toSmartList

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {
    private val backendContext = context.staticContext.backendContext

    private val perFile = context.staticContext.isPerFile
    private val es6mode = backendContext.es6mode

    private val className = context.getNameForClass(irClass)
    private val baseClass: IrType? = irClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }

    private val classNameUsedInsideDeclarationStatements = when {
        perFile -> JsName("$", true)
        else -> className
    }

    private val classNameRef = classNameUsedInsideDeclarationStatements.makeRef()

    private val classPrototypeRef by lazy(LazyThreadSafetyMode.NONE) { prototypeOf(classNameRef, context.staticContext) }
    private val baseClassRef by lazy(LazyThreadSafetyMode.NONE) { // Lazy in case was not collected by namer during JsClassGenerator construction
        if (baseClass != null && !baseClass.isAny()) baseClass.getClassRef(context.staticContext) else null
    }
    private val classModel = JsIrClassModel(irClass)
    private val classBlock = JsCompositeBlock()

    private val interfaceDefaultsBlock = when {
        perFile -> JsCompositeBlock()
        else -> classModel.preDeclarationBlock
    }

    private val jsUndefined by lazy(LazyThreadSafetyMode.NONE) { jsUndefined(context.staticContext) }

    fun generate(): JsStatement {
        return generateClassBlock().butIf(perFile) { it.wrapInFunction() }
    }

    private fun JsCompositeBlock.wrapInFunction(): JsStatement {
        val classHolder = JsVar(JsName("${className.ident}Class", true))
        val functionWrapper = JsFunction(emptyScope, JsBlock(), "lazy wrapper for classes in per-file").apply {
            name = className
            with(body.statements) {
                add(
                    JsIf(
                        JsAstUtils.equality(classHolder.name.makeRef(), jsUndefined),
                        JsBlock(
                            classModel.preDeclarationBlock.statements + statements + classModel.postDeclarationBlock.statements +
                                    JsAstUtils.assignment(classHolder.name.makeRef(), classNameRef).makeStmt()
                        )
                    )
                )
                add(JsReturn(classHolder.name.makeRef()))
            }
        }

        return JsCompositeBlock(interfaceDefaultsBlock.statements + listOf(JsVars(classHolder), functionWrapper.makeStmt())).also {
            classModel.preDeclarationBlock.statements.clear()
            classModel.postDeclarationBlock.statements.clear()
        }
    }

    private fun generateClassBlock(): JsCompositeBlock {
        assert(!irClass.isExpect)

        if (!es6mode) maybeGeneratePrimaryConstructor()

        // Properties might be lowered out of classes
        // We'll use IrSimpleFunction::correspondingProperty to collect them into set
        val properties = mutableSetOf<IrProperty>()

        val jsClass = JsClass(name = classNameUsedInsideDeclarationStatements, baseClass = baseClassRef)

        if (baseClass != null && !baseClass.isAny()) {
            jsClass.baseClass = baseClassRef
        }

        if (es6mode) {
            classModel.preDeclarationBlock.statements += jsClass.makeStmt()
        }

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrConstructor -> {
                    val constructor = declaration.accept(IrFunctionToJsTransformer(), context)
                    if (es6mode) {
                        jsClass.constructor = constructor.apply { name = null }
                    } else {
                        classBlock.statements += constructor.apply { name = classNameUsedInsideDeclarationStatements }.makeStmt()
                    }
                }
                is IrSimpleFunction -> {
                    properties.addIfNotNull(declaration.correspondingPropertySymbol?.owner)

                    if (es6mode) {
                        if (declaration.isEs6ConstructorReplacement && irClass.isInterface) continue
                        val (memberName, function) = generateMemberFunction(declaration)
                        function?.let { jsClass.members += it.escapedIfNeed() }
                        declaration.generateAssignmentIfMangled(memberName)
                    } else {
                        val (memberName, function) = generateMemberFunction(declaration)
                        val memberRef = jsElementAccess(memberName, classPrototypeRef)
                        function?.let { classBlock.statements += jsAssignment(memberRef, it.apply { name = null }).makeStmt() }
                        declaration.generateAssignmentIfMangled(memberName)
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

        fun IrSimpleFunction.hasImplicitParameters(): Boolean =
            parameters.any { it.kind == IrParameterKind.ExtensionReceiver || it.kind == IrParameterKind.Context }

        if (!irClass.isInterface) {
            for (property in properties) {
                if (property.getter?.hasImplicitParameters() == true || property.setter?.hasImplicitParameters() == true)
                    continue

                if (!property.visibility.isPublicAPI || property.isSimpleProperty || property.isJsExportIgnore())
                    continue

                if (
                    property.isFakeOverride &&
                    !property.isAllowedFakeOverriddenDeclaration(backendContext)
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
                // The exception is the case when we override val with var,
                // so we need to regenerate `defineProperty` with setter.
                // P.S. If the overridden property is owned by an interface - we should generate defineProperty
                // for overridden property in the first class which overrides those properties
                val hasOverriddenExportedInterfaceProperties = overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() }
                val hasOverriddenExternalInterfaceProperties = overriddenSymbols.any { it.owner.isDefinedInsideExternalInterface() }

                val getterOverridesExternal = overriddenSymbols.any { it.owner.realOverrideTarget.isEffectivelyExternal() }
                val thereIsNewlyIntroducedExternalSetter =
                    property.overridesExternal() && property.isVar && property.overriddenSymbols.all { !it.owner.isVar }

                val overriddenExportedGetter = overriddenSymbols.isNotEmpty() &&
                        property.getter?.isOverriddenExported(backendContext) == true

                val noOverriddenExportedSetter = property.setter?.isOverriddenExported(backendContext) == false

                val thereIsPropertyJsName = property.getJsName() != null
                val classIsExported = irClass.isExported(backendContext)
                val ownProperty = overriddenSymbols.isEmpty()
                val ownExportedSetter = overriddenExportedGetter && noOverriddenExportedSetter
                val needsOverride = ownExportedSetter || property.isOverriddenEnumProperty(backendContext)

                if (classIsExported &&
                    (ownProperty || needsOverride) ||
                    hasOverriddenExportedInterfaceProperties ||
                    hasOverriddenExternalInterfaceProperties ||
                    getterOverridesExternal ||
                    thereIsNewlyIntroducedExternalSetter ||
                    thereIsPropertyJsName
                ) {
                    val propertyName = context.getNameForProperty(property)

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
                        .getOrGenerateIfFinalOrEs6Mode {
                            propertyAccessorForwarder("getter forwarder") {
                                JsReturn(JsInvocation(it))
                            }
                        }

                    // We export setters in 4 cases:
                    // 1. The class is exported and the setter its own
                    // 2. There is @JsName annotation on property
                    // 3. The setter is a newly introduced to an overridden external property
                    // 4. The setter is an overridden setter defined in an external/exported interface
                    val setterForwarder = property.setter
                        ?.takeIf { setter ->
                            (classIsExported && (ownProperty || ownExportedSetter)) || thereIsPropertyJsName || thereIsNewlyIntroducedExternalSetter ||
                                    setter.overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() || it.owner.isDefinedInsideExternalInterface() }
                        }
                        .getOrGenerateIfFinalOrEs6Mode {
                            val setterArgName = JsName("value", false)
                            propertyAccessorForwarder("setter forwarder") {
                                JsInvocation(it, JsNameRef(setterArgName)).makeStmt()
                            }?.apply {
                                parameters.add(JsParameter(setterArgName))
                            }
                        }

                    if (es6mode) {
                        jsClass.members += listOfNotNull(
                            (getterForwarder as? JsFunction)?.apply {
                                name = propertyName
                                modifiers.add(JsFunction.Modifier.GET)
                            },
                            (setterForwarder as? JsFunction)?.apply {
                                name = propertyName
                                modifiers.add(JsFunction.Modifier.SET)
                            }
                        )
                    } else {
                        classModel.postDeclarationBlock.statements += JsExpressionStatement(
                            defineProperty(classPrototypeRef, propertyName.ident, getterForwarder, setterForwarder, context.staticContext)
                        )
                    }
                }
            }
        }

        val metadataPlace = if (es6mode) classModel.postDeclarationBlock else classModel.preDeclarationBlock

        metadataPlace.statements += generateInitMetadataCall()
        context.staticContext.classModels[irClass.symbol] = classModel

        return classBlock
    }

    private inline fun IrSimpleFunction?.getOrGenerateIfFinalOrEs6Mode(generateFunc: IrSimpleFunction.() -> JsFunction?): JsExpression? {
        if (this == null) return null
        return if (!es6mode && modality == Modality.FINAL) accessorRef() else generateFunc()
    }

    private fun IrSimpleFunction.isDefinedInsideExportedInterface(): Boolean {
        val irClass = parentClassOrNull ?: return false
        if (isJsExportIgnore() || correspondingPropertySymbol?.owner?.isJsExportIgnore() == true) return false
        if (!isFakeOverride) return irClass.isExportedInterface(backendContext)
        return overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() }
    }

    private fun IrSimpleFunction.isDefinedInsideExternalInterface(): Boolean {
        val irClass = parentClassOrNull ?: return false
        if (isEffectivelyExternal()) return irClass.isInterface
        if (!isFakeOverride && irClass.isClass) return false
        return overriddenSymbols.any { it.owner.isDefinedInsideExternalInterface() }
    }

    private fun IrSimpleFunction.accessorRef(): JsNameRef? =
        when (visibility) {
            DescriptorVisibilities.PRIVATE -> null
            else -> JsNameRef(
                context.getNameForMemberFunction(this),
                classPrototypeRef
            )
        }

    private fun IrSimpleFunction.generateAssignmentIfMangled(memberName: JsName) {
        if (
            irClass.isExported(backendContext) &&
            visibility.isPublicAPI && hasMangledName() &&
            correspondingPropertySymbol == null
        ) {
            classBlock.statements += jsAssignment(prototypeAccessRef(), jsElementAccess(memberName, classPrototypeRef)).makeStmt()
        }
    }

    private fun IrSimpleFunction.hasMangledName(): Boolean {
        return getJsName() == null && !name.asString().isValidES5Identifier()
    }

    private fun IrSimpleFunction.prototypeAccessRef(): JsExpression {
        return jsElementAccess(name.asString(), classPrototypeRef)
    }

    private fun IrClass.shouldCopyFrom(): Boolean {
        if (!isInterface || isEffectivelyExternal()) {
            return false
        }

        // Do not copy an interface method if the interface is already a parent of the base class,
        // as the method will already be copied from the interface into the base class
        val superIrClass = baseClass?.classOrNull?.owner ?: return true
        return !superIrClass.isSubclassOf(this)
    }

    private fun generateMemberFunction(declaration: IrSimpleFunction): Pair<JsName, JsFunction?> {
        val memberName = context.getNameForMemberFunction(declaration.realOverrideTarget)

        if (declaration.isReal && declaration.body != null) {
            val translatedFunction: JsFunction = declaration.accept(IrFunctionToJsTransformer(), context)
            assert(!declaration.isStaticMethodOfClass)

            if (irClass.isInterface) {
                interfaceDefaultsBlock.statements += translatedFunction.makeStmt()
                return Pair(memberName, null)
            }

            return Pair(memberName, translatedFunction)
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
                        classModel.postDeclarationBlock.statements += jsAssignment(
                            jsElementAccess(memberName, classPrototypeRef),
                            reference
                        ).makeStmt()
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

        return Pair(memberName, null)
    }

    private fun maybeGeneratePrimaryConstructor() {
        if (!irClass.declarations.any { it is IrConstructor }) {
            val func = JsFunction(emptyScope, JsBlock(), "Ctor for ${irClass.name}")
            func.name = classNameUsedInsideDeclarationStatements
            classBlock.statements += func.makeStmt()
        }
    }

    private fun generateInitMetadataCall(): JsStatement {
        val ctor = classNameRef
        val parent = baseClassRef?.takeIf { !es6mode }
        val name = generateSimpleName()
        val interfaces = generateInterfacesList()
        val defaultConstructor = runIf(irClass.isClass, ::findDefaultConstructor)
        val associatedObjectKey = generateAssociatedObjectKey()
        val associatedObjects = generateAssociatedObjects()
        val suspendArity = generateSuspendArity()

        var metadataKind: RuntimeMetadataKind? = null
        if (defaultConstructor == null && associatedObjectKey == null && associatedObjects == null) {
            metadataKind = when {
                irClass.isCompanion && name?.value == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString() -> RuntimeMetadataKind.COMPANION_OBJECT
                !irClass.isClass -> null
                irClass.origin == WebCallableReferenceLowering.LAMBDA_IMPL -> RuntimeMetadataKind.LAMBDA
                irClass.origin == WebCallableReferenceLowering.FUNCTION_REFERENCE_IMPL -> RuntimeMetadataKind.FUNCTION_REFERENCE
                irClass.origin == AbstractSuspendFunctionsLowering.DECLARATION_ORIGIN_COROUTINE_IMPL -> RuntimeMetadataKind.COROUTINE
                else -> null
            }
        }

        if (metadataKind == null) {
            metadataKind = when {
                irClass.isInterface -> RuntimeMetadataKind.INTERFACE
                irClass.isObject -> RuntimeMetadataKind.OBJECT
                else -> RuntimeMetadataKind.CLASS
            }
        }

        val initMetadataSymbol = backendContext.intrinsics.getInitMetadataSymbol(metadataKind)!!

        return if (metadataKind.isSpecial) {
            initMetadataSymbol.invokeWithoutNullArgs(ctor, parent, interfaces, suspendArity)
        } else {
            initMetadataSymbol.invokeWithoutNullArgs(
                ctor,
                name,
                defaultConstructor,
                parent,
                interfaces,
                suspendArity,
                associatedObjectKey,
                associatedObjects
            )
        }
    }

    private fun IrType.asConstructorRef(): JsExpression? {
        val ownerSymbol = classOrNull?.takeIf {
            !isAny() && !isFunctionType() && !it.owner.isEffectivelyExternal()
        } ?: return null

        return ownerSymbol.owner.getClassRef(context.staticContext)
    }

    private fun IrType.isFunctionType() = isFunctionOrKFunction() || isSuspendFunctionOrKFunction()

    private fun generateSimpleName(): JsStringLiteral? {
        return irClass.name.takeIf { !it.isSpecial }?.let { JsStringLiteral(it.identifier) }
    }

    private fun IrSimpleFunctionSymbol.invokeWithoutNullArgs(vararg arguments: JsExpression?): JsStatement {
        return JsInvocation(
            JsNameRef(context.getNameForStaticFunction(owner)),
            arguments.dropLastWhile { it == null }.memoryOptimizedMap { it ?: jsUndefined }
        ).makeStmt()
    }

    private fun generateInterfacesList(): JsArrayLiteral? {
        val listRef = irClass.superTypes
            .filter { it.classOrNull?.owner?.isExternal != true }
            .takeIf { it.size > 1 || it.singleOrNull() != baseClass }
            ?.mapNotNull { it.asConstructorRef() }
            ?.takeIf { it.isNotEmpty() } ?: return null
        return JsArrayLiteral(listRef.toSmartList())
    }

    private fun findDefaultConstructor(): JsNameRef? {
        return when (val defaultConstructor = irClass.findDefaultConstructorForReflection()) {
            is IrConstructor -> context.getNameForConstructor(defaultConstructor).makeRef()
            is IrSimpleFunction -> when {
                es6mode -> JsNameRef(context.getNameForMemberFunction(defaultConstructor), classNameRef)
                else -> context.getNameForStaticFunction(defaultConstructor).makeRef()
            }
            null -> null
        }
    }

    private fun generateSuspendArity(): JsArrayLiteral? {
        val invokeFunctions = irClass.suspendArityStore ?: return null
        val arity = invokeFunctions
            .map { it.nonDispatchParameters.size }
            .distinct()
            .map { JsIntLiteral(it) }

        return JsArrayLiteral(arity.toSmartList()).takeIf { arity.isNotEmpty() }
    }

    private fun generateAssociatedObjectKey(): JsExpression? {
        if (!irClass.isAssociatedObjectAnnotatedAnnotation) return null
        return if (backendContext.incrementalCacheEnabled) {
            JsInvocation(
                context.getNameForStaticFunction(backendContext.intrinsics.nextAssociatedObjectId.owner).makeRef(),
            )
        } else {
            val key = backendContext.nextAssociatedObjectKey++
            irClass.associatedObjectKey = key
            JsIntLiteral(key)
        }
    }

    private fun generateAssociatedObjects(): JsExpression? {
        val associatedObjects = irClass.annotations.mapNotNull { annotation ->
            val annotationClass = annotation.symbol.owner.constructedClass
            val objectGetInstanceFunction = annotation.associatedObject()?.objectGetInstanceFunction ?: return@mapNotNull null
            annotationClass to context.staticContext.getNameForStaticFunction(objectGetInstanceFunction).makeRef()
        }

        if (associatedObjects.isEmpty()) return null

        return when {
            !backendContext.incrementalCacheEnabled -> {
                JsObjectLiteral(
                    associatedObjects
                        .map { (key, objectGetInstanceFunction) ->
                            JsPropertyInitializer(JsIntLiteral(key.associatedObjectKey!!), objectGetInstanceFunction)
                        }
                        .toSmartList()
                )
            }
            es6mode -> {
                JsObjectLiteral(
                    associatedObjects
                        .map { (key, objectGetInstanceFunction) ->
                            JsPropertyInitializer(
                                JsInvocation(
                                    context.staticContext.getNameForStaticFunction(backendContext.intrinsics.getAssociatedObjectId.owner).makeRef(),
                                    key.getClassRef(context.staticContext),
                                ),
                                objectGetInstanceFunction
                            )
                        }
                        .toSmartList()
                )
            }
            else -> {
                // In ES5 object literals don't support computed keys, so we have to invoke a helper function to construct the associated
                // object map.
                JsInvocation(
                    context.staticContext.getNameForStaticFunction(backendContext.intrinsics.makeAssociatedObjectMapES5.owner).makeRef(),
                    JsArrayLiteral(
                        associatedObjects.flatMap { (key, objectGetInstanceFunction) ->
                            listOf(key.getClassRef(context.staticContext), objectGetInstanceFunction)
                        }.toSmartList()
                    )
                )
            }
        }
    }
}

fun JsFunction.escapedIfNeed(): JsFunction {
    if (name?.ident?.isValidES5Identifier() == false) {
        name = JsName("'${name.ident}'", name.isTemporary)
    }
    return this

}

fun IrSimpleFunction.overriddenStableProperty(context: JsIrBackendContext): Boolean {
    val property = correspondingPropertySymbol!!.owner

    if (property.isOverriddenExported(context)) {
        return isOverriddenExported(context)
    }

    return property.getJsName() != null || overridesExternal()
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true

private fun IrClassSymbol.existsInRuntime(): Boolean {
    return !owner.isEffectivelyExternal() || !owner.isInterface
}

class JsIrClassModel(val klass: IrClass) {
    val superClasses = klass.superTypes.memoryOptimizedMapNotNull {
        (it.classifierOrNull as IrClassSymbol).takeIf(IrClassSymbol::existsInRuntime)
    }

    val preDeclarationBlock = JsCompositeBlock()
    val postDeclarationBlock = JsCompositeBlock()
}

class JsIrIcClassModel(val superClasses: List<JsName>) {
    val preDeclarationBlock = JsCompositeBlock()
    val postDeclarationBlock = JsCompositeBlock()
}

/**
 * Each `@AssociatedObjectKey`-annotated annotation class is assigned a unique integer.
 *
 * This property is only used in non-incremental compilation.
 * When compiling incrementally, these integers are assigned at runtime.
 */
private var IrClass.associatedObjectKey: Int? by irAttribute(copyByDefault = false)
