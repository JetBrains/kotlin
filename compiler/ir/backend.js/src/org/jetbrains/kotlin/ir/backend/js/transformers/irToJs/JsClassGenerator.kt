/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isAllowedFakeOverriddenDeclaration
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.export.isOverriddenExported
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.toSmartList

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {
    private val className = context.getNameForClass(irClass)
    private val classNameRef = className.makeRef()
    private val baseClass: IrType? = irClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }

    private val classPrototypeRef by lazy(LazyThreadSafetyMode.NONE) { prototypeOf(classNameRef, context.staticContext) }
    private val baseClassRef by lazy(LazyThreadSafetyMode.NONE) { // Lazy in case was not collected by namer during JsClassGenerator construction
        if (baseClass != null && !baseClass.isAny()) baseClass.getClassRef(context) else null
    }
    private val classBlock = JsCompositeBlock()
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
                        declaration.accept(IrFunctionToJsTransformer(), context).let { fn ->
                            if (fn.body.statements.any { it !is JsEmpty && !it.isSimpleSuperCall(fn) }) {
                                jsClass.constructor = fn
                            }
                        }
                    } else {
                        classBlock.statements += declaration.accept(transformer, context)
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

        if (!irClass.isInterface) {
            for (property in properties) {
                if (property.getter?.extensionReceiverParameter != null || property.setter?.extensionReceiverParameter != null)
                    continue

                if (!property.visibility.isPublicAPI || property.isSimpleProperty || property.isJsExportIgnore())
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

                val backendContext = context.staticContext.backendContext

                // Don't generate `defineProperty` if the property overrides a property from an exported class,
                // because we've already generated `defineProperty` for the base class property.
                // In other words, we only want to generate `defineProperty` once for each property.
                // The exception is case when we override val with var,
                // so we need regenerate `defineProperty` with setter.
                // P.S. If the overridden property is owned by an interface - we should generate defineProperty
                // for overridden property in the first class which override those properties
                val hasOverriddenExportedInterfaceProperties = overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() }
                        && !overriddenSymbols.any { it.owner.parentClassOrNull.isExportedClass(backendContext) }

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
                        .takeIf { it.shouldExportAccessor(context.staticContext.backendContext) }
                        .getOrGenerateIfFinalOrEs6Mode {
                            propertyAccessorForwarder("getter forwarder") {
                                JsReturn(JsInvocation(it))
                            }
                        }

                    val setterForwarder = property.setter
                        .takeIf { it.shouldExportAccessor(context.staticContext.backendContext) }
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
                        classBlock.statements += JsExpressionStatement(
                            defineProperty(classPrototypeRef, propertyName.ident, getterForwarder, setterForwarder, context.staticContext)
                        )
                    }
                }
            }
        }

        val metadataPlace = if (es6mode) classModel.postDeclarationBlock else classModel.preDeclarationBlock

        metadataPlace.statements += generateSetMetadataCall()
        context.staticContext.classModels[irClass.symbol] = classModel

        return classBlock
    }

    private inline fun IrSimpleFunction?.getOrGenerateIfFinalOrEs6Mode(generateFunc: IrSimpleFunction.() -> JsFunction?): JsExpression? {
        if (this == null) return null
        return if (!es6mode && modality == Modality.FINAL) accessorRef() else generateFunc()
    }

    private fun IrSimpleFunction.isDefinedInsideExportedInterface(): Boolean {
        if (isJsExportIgnore() || correspondingPropertySymbol?.owner?.isJsExportIgnore() == true) return false
        return (!isFakeOverride && parentClassOrNull.isExportedInterface(context.staticContext.backendContext)) ||
                overriddenSymbols.any { it.owner.isDefinedInsideExportedInterface() }
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
            irClass.isExported(context.staticContext.backendContext) &&
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
                classModel.preDeclarationBlock.statements += translatedFunction.makeStmt()
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
            func.name = className
            classBlock.statements += func.makeStmt()
        }
    }

    private fun generateSetMetadataCall(): JsStatement {
        val setMetadataFor = context.staticContext.backendContext.intrinsics.setMetadataForSymbol.owner

        val ctor = classNameRef
        val parent = baseClassRef?.takeIf { !es6mode }
        val name = generateSimpleName()
        val interfaces = generateInterfacesList()
        val metadataConstructor = getMetadataConstructor()
        val associatedObjectKey = generateAssociatedObjectKey()
        val associatedObjects = generateAssociatedObjects()
        val suspendArity = generateSuspendArity()

        val undefined = context.staticContext.backendContext.getVoid().accept(IrElementToJsExpressionTransformer(), context)

        return JsInvocation(
            JsNameRef(context.getNameForStaticFunction(setMetadataFor)),
            listOf(ctor, name, metadataConstructor, parent, interfaces, associatedObjectKey, associatedObjects, suspendArity)
                .dropLastWhile { it == null }
                .memoryOptimizedMap { it ?: undefined }
        ).makeStmt()

    }


    private fun IrType.asConstructorRef(): JsNameRef? {
        val ownerSymbol = classOrNull?.takeIf {
            !isAny() && !isFunctionType() && !it.owner.isEffectivelyExternal()
        } ?: return null

        return JsNameRef(context.getNameForClass(ownerSymbol.owner))
    }

    private fun IrType.isFunctionType() = isFunctionOrKFunction() || isSuspendFunctionOrKFunction()

    private fun generateSimpleName(): JsStringLiteral? {
        return irClass.name.takeIf { !it.isSpecial }?.let { JsStringLiteral(it.identifier) }
    }

    private fun getMetadataConstructor(): JsNameRef {
        val metadataConstructorSymbol = with(context.staticContext.backendContext.intrinsics) {
            when {
                irClass.isInterface -> metadataInterfaceConstructorSymbol
                irClass.isObject -> metadataObjectConstructorSymbol
                else -> metadataClassConstructorSymbol
            }
        }

        return JsNameRef(context.getNameForStaticFunction(metadataConstructorSymbol.owner))
    }

    private fun generateInterfacesList(): JsArrayLiteral? {
        val listRef = irClass.superTypes
            .filter { it.classOrNull?.owner?.isExternal != true }
            .takeIf { it.size > 1 || it.singleOrNull() != baseClass }
            ?.mapNotNull { it.asConstructorRef() }
            ?.takeIf { it.isNotEmpty() } ?: return null
        return JsArrayLiteral(listRef.toSmartList())
    }

    private fun generateSuspendArity(): JsArrayLiteral? {
        val invokeFunctions = context.staticContext.backendContext.mapping.suspendArityStore[irClass] ?: return null
        val arity = invokeFunctions
            .map { it.valueParameters.size }
            .distinct()
            .map { JsIntLiteral(it) }

        return JsArrayLiteral(arity.toSmartList()).takeIf { arity.isNotEmpty() }
    }

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
        }.toSmartList()

        return associatedObjects
            .takeIf { it.isNotEmpty() }
            ?.let { JsObjectLiteral(it) }
    }
}

fun JsFunction.escapedIfNeed(): JsFunction {
    if (name?.ident?.isValidES5Identifier() == false) {
        name = JsName("'${name.ident}'", name.isTemporary)
    }
    return this

}

fun JsStatement.isSimpleSuperCall(container: JsFunction): Boolean {
    if (this !is JsExpressionStatement) return false
    val invocation = expression as? JsInvocation ?: return false
    if (invocation.qualifier !is JsSuperRef || container.parameters.size != invocation.arguments.size) return false

    for (i in 0..container.parameters.lastIndex) {
        val declaredParameter = container.parameters[i]
        val providedParameter = (invocation.arguments[i] as? JsNameRef)?.takeIf { it.qualifier == null } ?: return false

        if (declaredParameter.name != providedParameter.name) {
            return false
        }
    }

    return true
}

fun IrSimpleFunction?.shouldExportAccessor(context: JsIrBackendContext): Boolean {
    if (this == null) return false

    if (parentAsClass.isExported(context)) return true

    return isAccessorOfOverriddenStableProperty(context)
}

fun IrSimpleFunction.overriddenStableProperty(context: JsIrBackendContext): Boolean {
    val property = correspondingPropertySymbol!!.owner

    if (property.isOverriddenExported(context)) {
        return isOverriddenExported(context)
    }

    return overridesExternal() || property.getJsName() != null
}

fun IrSimpleFunction.isAccessorOfOverriddenStableProperty(context: JsIrBackendContext): Boolean {
    return overriddenStableProperty(context) || correspondingPropertySymbol!!.owner.overridesExternal()
}

private fun IrOverridableDeclaration<*>.overridesExternal(): Boolean {
    if (this.isEffectivelyExternal()) return true

    return overriddenSymbols.any { (it.owner as IrOverridableDeclaration<*>).overridesExternal() }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true

class JsIrClassModel(val klass: IrClass) {
    val superClasses = klass.superTypes.memoryOptimizedMap { it.classifierOrNull as IrClassSymbol }

    val preDeclarationBlock = JsCompositeBlock()
    val postDeclarationBlock = JsCompositeBlock()
}

class JsIrIcClassModel(val superClasses: List<JsName>) {
    val preDeclarationBlock = JsCompositeBlock()
    val postDeclarationBlock = JsCompositeBlock()
}
