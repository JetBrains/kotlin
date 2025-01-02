/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isMappedIntrinsicCompanionObjectClassId
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

private var IrEnumEntry.declaringField: IrField? by irAttribute(copyByDefault = false)
private var IrProperty.staticBackingFields: IrField? by irAttribute(copyByDefault = false)
private var IrSimpleFunction.staticCompanionDeclarations: Pair<IrSimpleFunction, IrSimpleFunction>? by irAttribute(copyByDefault = false)

private var IrSimpleFunction.defaultImplsMethod: IrSimpleFunction? by irAttribute(copyByDefault = false)
private var IrClass.defaultImplsClass: IrClass? by irAttribute(copyByDefault = false)
private var IrSimpleFunction.classFakeOverrideReplacement: ClassFakeOverrideReplacement? by irAttribute(copyByDefault = false)
var IrSimpleFunction.originalFunctionForDefaultImpl: IrSimpleFunction? by irAttribute(copyByDefault = false)

private var IrClass.repeatedAnnotationSyntheticContainer: IrClass? by irAttribute(copyByDefault = false)

class JvmCachedDeclarations(
    private val context: JvmBackendContext,
    val fieldsForObjectInstances: CachedFieldsForObjectInstances,
) {
    fun getFieldForEnumEntry(enumEntry: IrEnumEntry): IrField =
        enumEntry::declaringField.getOrSetIfNull {
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
        return irProperty::staticBackingFields.getOrSetIfNull {
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
        jvmStaticFunction::staticCompanionDeclarations.getOrSetIfNull {
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
                    copyValueAndTypeParametersFrom(jvmStaticFunction)
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
            setSourceRange(target)
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
            parameters = target.nonDispatchParameters.map { it.copyTo(this) }
            if (!isStatic) {
                parameters = listOfNotNull(
                    thisReceiver?.copyTo(this, type = defaultType, kind = IrParameterKind.DispatchReceiver)
                ) + parameters
            }

            body = context.createIrBuilder(symbol).run {
                irExprBody(irCall(target).apply {
                    passTypeArgumentsFrom(this@proxy)

                    var dstIndex = 0
                    if (target.dispatchReceiverParameter != null) {
                        arguments[dstIndex++] = irGetField(null, getFieldForObjectInstance(target.parentAsClass))
                    }
                    for (param in parameters) {
                        if (param.kind == IrParameterKind.DispatchReceiver) continue
                        arguments[dstIndex++] = irGet(param)
                    }
                })
            }
        }

    fun getDefaultImplsFunction(interfaceFun: IrSimpleFunction, forCompatibilityMode: Boolean = false): IrSimpleFunction {
        val parent = interfaceFun.parentAsClass
        assert(parent.isJvmInterface) { "Parent of ${interfaceFun.dump()} should be interface" }
        assert(!forCompatibilityMode || interfaceFun.defaultImplsMethod == null) { "DefaultImpls stub in compatibility mode should be requested only once from interface lowering: ${interfaceFun.dump()}" }
        return interfaceFun::defaultImplsMethod.getOrSetIfNull {
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
                else interfaceFun.resolveFakeOverrideOrFail().origin

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
                typeParametersFromContext = parent.typeParameters,
                remapMultiFieldValueClassStructure = context::remapMultiFieldValueClassStructure
            ).also {
                it.copyCorrespondingPropertyFrom(interfaceFun)

                if (forCompatibilityMode && !interfaceFun.resolveFakeOverrideOrFail().origin.isSynthetic) {
                    context.createJvmIrBuilder(it.symbol).run {
                        it.annotations = it.annotations
                            .filterNot { it.symbol.owner.constructedClass.hasEqualFqName(DeprecationResolver.JAVA_DEPRECATED) }
                            .plus(irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag))
                    }
                }

                it.originalFunctionForDefaultImpl = interfaceFun
            }
        }
    }

    fun getDefaultImplsClass(interfaceClass: IrClass): IrClass =
        interfaceClass::defaultImplsClass.getOrSetIfNull {
            context.irFactory.buildClass {
                startOffset = interfaceClass.startOffset
                endOffset = interfaceClass.endOffset
                origin = JvmLoweredDeclarationOrigin.DEFAULT_IMPLS
                name = Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)
            }.apply {
                parent = interfaceClass
                createThisReceiverParameter()
            }
        }

    fun getClassFakeOverrideReplacement(fakeOverride: IrSimpleFunction): ClassFakeOverrideReplacement =
        if (!fakeOverride.isFakeOverride || fakeOverride.parentAsClass.isJvmInterface) {
            ClassFakeOverrideReplacement.None
        } else fakeOverride::classFakeOverrideReplacement.getOrSetIfNull {
            computeClassFakeOverrideReplacement(fakeOverride) ?: ClassFakeOverrideReplacement.None
        }

    private fun computeClassFakeOverrideReplacement(fakeOverride: IrSimpleFunction): ClassFakeOverrideReplacement? {
        val implementation = fakeOverride.findInterfaceImplementation(context.config.jvmDefaultMode, allowJvmDefault = true) ?: return null
        val newFunction = context.irFactory.createDefaultImplsRedirection(fakeOverride)
        context.remapMultiFieldValueClassStructure(fakeOverride, newFunction, parametersMappingOrNull = null)
        val superFunction = firstSuperMethodFromKotlin(newFunction, implementation).owner

        findDefaultImplsRedirection(implementation, newFunction, superFunction)?.let { return it }
        if (needsJvmDefaultCompatibilityBridge(fakeOverride)) {
            return ClassFakeOverrideReplacement.DefaultCompatibilityBridge(newFunction, superFunction)
        }
        return null
    }

    private fun findDefaultImplsRedirection(
        implementation: IrSimpleFunction, newFunction: IrSimpleFunction, superFunction: IrSimpleFunction,
    ): ClassFakeOverrideReplacement.DefaultImplsRedirection? {
        val callee =
            if (implementation.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB ||
                implementation.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                implementation.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)
            ) {
                return null
            } else if (implementation.isDefinitelyNotDefaultImplsMethod(context.config.jvmDefaultMode, implementation)) {
                val klass = newFunction.parentAsClass
                when (context.config.jvmDefaultMode) {
                    JvmDefaultMode.NO_COMPATIBILITY -> return null
                    JvmDefaultMode.ENABLE if klass.hasJvmDefaultNoCompatibilityAnnotation() -> return null
                    else -> superFunction
                }
            } else {
                getDefaultImplsFunction(superFunction)
            }

        return ClassFakeOverrideReplacement.DefaultImplsRedirection(newFunction, superFunction, callee)
    }

    // Generating the default compatibility bridge is only necessary if there's a risk that some other method will be called at runtime
    // by JVM when resolving the call to method from this class. This is only possible when this class has a superclass, where this method
    // is present as a bridge to DefaultImpls of some other interface.
    private fun needsJvmDefaultCompatibilityBridge(declaration: IrSimpleFunction): Boolean {
        var current: IrSimpleFunction? = declaration.overriddenSymbols.singleOrNull { it.owner.parentAsClass.isClass }?.owner
        while (current != null) {
            if (current.modality == Modality.ABSTRACT) return false

            if (current.findInterfaceImplementation(context.config.jvmDefaultMode) != null) return true

            current = current.overriddenSymbols.singleOrNull { it.owner.parentAsClass.isClass }?.owner
        }
        return false
    }

    fun getRepeatedAnnotationSyntheticContainer(annotationClass: IrClass): IrClass =
        annotationClass::repeatedAnnotationSyntheticContainer.getOrSetIfNull {
            val containerClass = context.irFactory.buildClass {
                kind = ClassKind.ANNOTATION_CLASS
                name = Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)
            }.apply {
                createThisReceiverParameter()
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

private var IrClass.fieldForObjectInstance: IrField? by irAttribute(copyByDefault = false)
private var IrClass.interfaceCompanionFieldForObjectInstance: IrField? by irAttribute(copyByDefault = false)

/*
    This class keeps track of singleton fields for instances of object classes.
 */
class CachedFieldsForObjectInstances(
    private val irFactory: IrFactory,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    fun getFieldForObjectInstance(singleton: IrClass): IrField =
        singleton::fieldForObjectInstance.getOrSetIfNull {
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
            singleton::interfaceCompanionFieldForObjectInstance.getOrSetIfNull {
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

