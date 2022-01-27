/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.copyCorrespondingPropertyFrom
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isMappedIntrinsicCompanionObjectClassId
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import java.util.concurrent.ConcurrentHashMap

class JvmCachedDeclarations(
    private val context: JvmBackendContext,
    val fieldsForObjectInstances: CachedFieldsForObjectInstances,
) {
    val syntheticAccessorGenerator = CachedSyntheticDeclarations(context)

    private val singletonFieldDeclarations = ConcurrentHashMap<IrSymbolOwner, IrField>()
    private val staticBackingFields = ConcurrentHashMap<IrProperty, IrField>()
    private val staticCompanionDeclarations = ConcurrentHashMap<IrSimpleFunction, Pair<IrSimpleFunction, IrSimpleFunction>>()

    private val defaultImplsMethods = ConcurrentHashMap<IrSimpleFunction, IrSimpleFunction>()
    private val defaultImplsClasses = ConcurrentHashMap<IrClass, IrClass>()
    private val defaultImplsRedirections = ConcurrentHashMap<IrSimpleFunction, IrSimpleFunction>()
    private val defaultImplsOriginalMethods = ConcurrentHashMap<IrSimpleFunction, IrSimpleFunction>()

    private val repeatedAnnotationSyntheticContainers = ConcurrentHashMap<IrClass, IrClass>()

    fun getFieldForEnumEntry(enumEntry: IrEnumEntry): IrField =
        singletonFieldDeclarations.getOrPut(enumEntry) {
            context.irFactory.buildField {
                setSourceRange(enumEntry)
                name = enumEntry.name
                type = enumEntry.parentAsClass.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY
                isFinal = true
                isStatic = true
            }.apply {
                parent = enumEntry.parent
            }
        }

    fun getFieldForObjectInstance(singleton: IrClass): IrField = fieldsForObjectInstances.getFieldForObjectInstance(singleton)

    fun getPrivateFieldForObjectInstance(singleton: IrClass): IrField = fieldsForObjectInstances.getPrivateFieldForObjectInstance(singleton)

    fun getStaticBackingField(irProperty: IrProperty): IrField? {
        // Only fields defined directly in objects should be made static.
        // Fake overrides never point to those, as objects are final.
        if (irProperty.isFakeOverride) return null
        val oldField = irProperty.backingField ?: return null
        val oldParent = irProperty.parent as? IrClass ?: return null
        if (!oldParent.isObject) return null
        return staticBackingFields.getOrPut(irProperty) {
            context.irFactory.buildField {
                updateFrom(oldField)
                name = oldField.name
                isStatic = true
            }.apply {
                // We don't move fields to interfaces unless all fields are annotated with @JvmField.
                // It is an error to annotate only some of the fields of an interface companion with
                // @JvmField, so checking the current field only should be enough.
                val hasJvmField = oldField.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
                val shouldMoveFields = oldParent.isCompanion && (!oldParent.parentAsClass.isJvmInterface || hasJvmField)
                if (shouldMoveFields) {
                   parent = oldParent.parentAsClass
                   val isPrivate = DescriptorVisibilities.isPrivate(oldField.visibility)
                   val parentIsPrivate = DescriptorVisibilities.isPrivate(oldParent.visibility)
                   annotations = if (parentIsPrivate && !isPrivate) {
                       context.createJvmIrBuilder(this.symbol).run {
                           filterOutAnnotations(
                               DeprecationResolver.JAVA_DEPRECATED,
                               oldField.annotations
                           ) + irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
                       }
                   } else {
                       oldField.annotations
                   }
                } else {
                    parent = oldParent
                    annotations = oldField.annotations
                }
                initializer = oldField.initializer?.patchDeclarationParents(this)
                oldField.replaceThisByStaticReference(fieldsForObjectInstances, oldParent, oldParent.thisReceiver!!)
                origin = if (irProperty.parentAsClass.isCompanion) JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD else origin
            }
        }
    }

    fun getStaticAndCompanionDeclaration(jvmStaticFunction: IrSimpleFunction): Pair<IrSimpleFunction, IrSimpleFunction> =
        staticCompanionDeclarations.getOrPut(jvmStaticFunction) {
            val companion = jvmStaticFunction.parentAsClass
            assert(companion.isCompanion)
            if (jvmStaticFunction.isExternal) {
                // We move external functions to the enclosing class and potentially add accessors there.
                // The JVM backend also adds accessors in the companion object, but these are superfluous.
                val staticExternal = context.irFactory.buildFun {
                    updateFrom(jvmStaticFunction)
                    name = jvmStaticFunction.name
                    returnType = jvmStaticFunction.returnType
                }.apply {
                    parent = companion.parent
                    copyAttributes(jvmStaticFunction)
                    copyAnnotationsFrom(jvmStaticFunction)
                    copyCorrespondingPropertyFrom(jvmStaticFunction)
                    copyParameterDeclarationsFrom(jvmStaticFunction)
                    dispatchReceiverParameter = null
                    metadata = jvmStaticFunction.metadata
                }
                staticExternal to companion.makeProxy(staticExternal, isStatic = false)
            } else {
                companion.parentAsClass.makeProxy(jvmStaticFunction, isStatic = true) to jvmStaticFunction
            }
        }

    private fun IrClass.makeProxy(target: IrSimpleFunction, isStatic: Boolean) =
        context.irFactory.buildFun {
            returnType = target.returnType
            origin = JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
            // The proxy needs to have the same name as what it is targeting. If that is a property accessor,
            // we need to make sure that the name is mapped correctly. The static method is not a property accessor,
            // so we do not have a property to link it up to. Therefore, we compute the right name now.
            name = Name.identifier(context.defaultMethodSignatureMapper.mapFunctionName(target))
            modality = if (isInterface) Modality.OPEN else target.modality
            // Since we already mangle the name above we need to reset internal visibilities to public in order
            // to avoid mangling the same name twice.
            visibility = when (target.visibility) {
                DescriptorVisibilities.INTERNAL ->
                    DescriptorVisibilities.PUBLIC
                DescriptorVisibilities.PROTECTED -> {
                    // Required to properly create accessors to protected static companion object member
                    // when this member is referenced in subclass.
                    if (isStatic)
                        JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
                    else
                        DescriptorVisibilities.PROTECTED
                }
                else ->
                    target.visibility
            }
            isSuspend = target.isSuspend
        }.apply proxy@{
            parent = this@makeProxy
            copyAttributes(target)
            copyTypeParametersFrom(target)
            copyAnnotationsFrom(target)
            if (!isStatic) {
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)
            }
            extensionReceiverParameter = target.extensionReceiverParameter?.copyTo(this)
            valueParameters = target.valueParameters.map { it.copyTo(this) }

            body = context.createIrBuilder(symbol).run {
                irExprBody(irCall(target).apply {
                    passTypeArgumentsFrom(this@proxy)
                    if (target.dispatchReceiverParameter != null) {
                        dispatchReceiver = irGetField(null, getFieldForObjectInstance(target.parentAsClass))
                    }
                    extensionReceiverParameter?.let { extensionReceiver = irGet(it) }
                    for ((i, valueParameter) in valueParameters.withIndex()) {
                        putValueArgument(i, irGet(valueParameter))
                    }
                })
            }
        }

    fun getDefaultImplsFunction(interfaceFun: IrSimpleFunction, forCompatibilityMode: Boolean = false): IrSimpleFunction {
        val parent = interfaceFun.parentAsClass
        assert(parent.isJvmInterface) { "Parent of ${interfaceFun.dump()} should be interface" }
        assert(!forCompatibilityMode || !defaultImplsMethods.containsKey(interfaceFun)) { "DefaultImpls stub in compatibility mode should be requested only once from interface lowering: ${interfaceFun.dump()}" }
        return defaultImplsMethods.getOrPut(interfaceFun) {
            val defaultImpls = getDefaultImplsClass(interfaceFun.parentAsClass)

            // If `interfaceFun` is not a real implementation, then we're generating stubs in a descendant
            // interface's DefaultImpls. For example,
            //
            //     interface I1 { fun f() { ... } }
            //     interface I2 : I1
            //
            // is supposed to allow using `I2.DefaultImpls.f` as if it was inherited from `I1.DefaultImpls`.
            // The classes are not actually related and `I2.DefaultImpls.f` is not a fake override but a bridge.
            val defaultImplsOrigin =
                if (!forCompatibilityMode && !interfaceFun.isFakeOverride) interfaceFun.origin
                else interfaceFun.resolveFakeOverride()!!.origin

            // Interface functions are public or private, with one exception: clone in Cloneable, which is protected.
            // However, Cloneable has no DefaultImpls, so this merely replicates the incorrect behavior of the old backend.
            // We should rather not generate a bridge to clone when interface inherits from Cloneable at all.
            val defaultImplsVisibility =
                if (DescriptorVisibilities.isPrivate(interfaceFun.visibility))
                    DescriptorVisibilities.PRIVATE
                else
                    DescriptorVisibilities.PUBLIC

            context.irFactory.createStaticFunctionWithReceivers(
                defaultImpls, interfaceFun.name, interfaceFun,
                dispatchReceiverType = parent.defaultType,
                origin = defaultImplsOrigin,
                // Old backend doesn't generate ACC_FINAL on DefaultImpls methods.
                modality = Modality.OPEN,
                visibility = defaultImplsVisibility,
                isFakeOverride = false,
                typeParametersFromContext = parent.typeParameters
            ).also {
                it.copyCorrespondingPropertyFrom(interfaceFun)

                if (forCompatibilityMode && !interfaceFun.resolveFakeOverride()!!.origin.isSynthetic) {
                    context.createJvmIrBuilder(it.symbol).run {
                        it.annotations = it.annotations
                            .filterNot { it.symbol.owner.constructedClass.hasEqualFqName(DeprecationResolver.JAVA_DEPRECATED) }
                            .plus(irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag))
                    }
                }

                defaultImplsOriginalMethods[it] = interfaceFun
            }
        }
    }

    fun getOriginalFunctionForDefaultImpl(defaultImplFun: IrSimpleFunction) =
        defaultImplsOriginalMethods[defaultImplFun]

    fun getDefaultImplsClass(interfaceClass: IrClass): IrClass =
        defaultImplsClasses.getOrPut(interfaceClass) {
            context.irFactory.buildClass {
                startOffset = interfaceClass.startOffset
                endOffset = interfaceClass.endOffset
                origin = JvmLoweredDeclarationOrigin.DEFAULT_IMPLS
                name = Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)
            }.apply {
                parent = interfaceClass
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }
        }

    fun getDefaultImplsRedirection(fakeOverride: IrSimpleFunction): IrSimpleFunction =
        defaultImplsRedirections.getOrPut(fakeOverride) {
            assert(fakeOverride.isFakeOverride)
            val irClass = fakeOverride.parentAsClass
            val mfvcReplacementStructure = context.multiFieldValueClassReplacements.bindingNewFunctionToParameterTemplateStructure
            val redirectFunction = context.irFactory.buildFun {
                origin = JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE
                name = fakeOverride.name
                visibility = fakeOverride.visibility
                modality = fakeOverride.modality
                returnType = fakeOverride.returnType
                isInline = fakeOverride.isInline
                isExternal = false
                isTailrec = false
                isSuspend = fakeOverride.isSuspend
                isOperator = fakeOverride.isOperator
                isInfix = fakeOverride.isInfix
                isExpect = false
                isFakeOverride = false
            }.apply {
                parent = irClass
                overriddenSymbols = fakeOverride.overriddenSymbols
                copyParameterDeclarationsFrom(fakeOverride)
                // The fake override's dispatch receiver has the same type as the real declaration's,
                // i.e. some superclass of the current class. This is not good for accessibility checks.
                dispatchReceiverParameter?.type = irClass.defaultType
                annotations = fakeOverride.annotations
                copyCorrespondingPropertyFrom(fakeOverride)
            }
            mfvcReplacementStructure[fakeOverride]?.let { mfvcReplacementStructure[redirectFunction] = it }
            redirectFunction
        }

    fun getRepeatedAnnotationSyntheticContainer(annotationClass: IrClass): IrClass =
        repeatedAnnotationSyntheticContainers.getOrPut(annotationClass) {
            val containerClass = context.irFactory.buildClass {
                kind = ClassKind.ANNOTATION_CLASS
                name = Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)
            }.apply {
                createImplicitParameterDeclarationWithWrappedDescriptor()
                parent = annotationClass
                superTypes = listOf(context.irBuiltIns.annotationType)
            }

            val propertyName = Name.identifier("value")
            val propertyType = context.irBuiltIns.arrayClass.typeWith(annotationClass.typeWith())

            containerClass.addConstructor {
                isPrimary = true
            }.apply {
                addValueParameter(propertyName, propertyType)
            }

            containerClass.addProperty {
                name = propertyName
            }.apply property@{
                backingField = context.irFactory.buildField {
                    name = propertyName
                    type = propertyType
                }.apply {
                    parent = containerClass
                    correspondingPropertySymbol = this@property.symbol
                }
                addDefaultGetter(containerClass, context.irBuiltIns)
            }

            containerClass.annotations = annotationClass.annotations
                .filter {
                    it.isAnnotationWithEqualFqName(StandardNames.FqNames.retention) ||
                            it.isAnnotationWithEqualFqName(StandardNames.FqNames.target)
                }
                .map { it.deepCopyWithSymbols(containerClass) } +
                    context.createJvmIrBuilder(containerClass.symbol).irCall(context.ir.symbols.repeatableContainer.constructors.single())

            containerClass
        }
}

/*
    This class keeps track of singleton fields for instances of object classes.
 */
class CachedFieldsForObjectInstances(
    private val irFactory: IrFactory,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    private val singletonFieldDeclarations = ConcurrentHashMap<IrSymbolOwner, IrField>()
    private val interfaceCompanionFieldDeclarations = ConcurrentHashMap<IrSymbolOwner, IrField>()

    fun getFieldForObjectInstance(singleton: IrClass): IrField =
        singletonFieldDeclarations.getOrPut(singleton) {
            val originalVisibility = singleton.visibility
            val isNotMappedCompanion = singleton.isCompanion && !singleton.isMappedIntrinsicCompanionObject()
            val useProperVisibilityForCompanion =
                languageVersionSettings.supportsFeature(LanguageFeature.ProperVisibilityForCompanionObjectInstanceField)
                        && singleton.isCompanion
                        && !singleton.parentAsClass.isInterface
            irFactory.buildField {
                name = if (isNotMappedCompanion) singleton.name else Name.identifier(JvmAbi.INSTANCE_FIELD)
                type = singleton.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                isFinal = true
                isStatic = true
                visibility = when {
                    !useProperVisibilityForCompanion -> DescriptorVisibilities.PUBLIC
                    originalVisibility == DescriptorVisibilities.PROTECTED -> JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY
                    else -> originalVisibility
                }

            }.apply {
                parent = if (isNotMappedCompanion) singleton.parent else singleton
            }
        }

    private fun IrClass.isMappedIntrinsicCompanionObject() =
        isCompanion && classId?.let { CompanionObjectMapping.isMappedIntrinsicCompanionObjectClassId(it) } == true

    fun getPrivateFieldForObjectInstance(singleton: IrClass): IrField =
        if (singleton.isCompanion && singleton.parentAsClass.isJvmInterface)
            interfaceCompanionFieldDeclarations.getOrPut(singleton) {
                irFactory.buildField {
                    name = Name.identifier("\$\$INSTANCE")
                    type = singleton.defaultType
                    origin = JvmLoweredDeclarationOrigin.INTERFACE_COMPANION_PRIVATE_INSTANCE
                    isFinal = true
                    isStatic = true
                    visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                }.apply {
                    parent = singleton
                }
            }
        else
            getFieldForObjectInstance(singleton)

}

