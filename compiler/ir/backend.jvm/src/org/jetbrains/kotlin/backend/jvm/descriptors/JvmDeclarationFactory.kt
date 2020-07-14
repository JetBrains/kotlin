/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.MethodSignatureMapper
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.copyCorrespondingPropertyFrom
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.builtins.CompanionObjectMapping.isMappedIntrinsicCompanionObject
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import java.util.*

class JvmDeclarationFactory(
    private val context: JvmBackendContext,
    private val methodSignatureMapper: MethodSignatureMapper,
    private val languageVersionSettings: LanguageVersionSettings
) : DeclarationFactory {
    private val singletonFieldDeclarations = HashMap<IrSymbolOwner, IrField>()
    private val interfaceCompanionFieldDeclarations = HashMap<IrSymbolOwner, IrField>()
    private val outerThisDeclarations = HashMap<IrClass, IrField>()
    private val innerClassConstructors = HashMap<IrConstructor, IrConstructor>()
    private val originalInnerClassPrimaryConstructorByClass = HashMap<IrClass, IrConstructor>()
    private val staticBackingFields = HashMap<IrProperty, IrField>()

    private val defaultImplsMethods = HashMap<IrSimpleFunction, IrSimpleFunction>()
    private val defaultImplsClasses = HashMap<IrClass, IrClass>()
    private val defaultImplsRedirections = HashMap<IrSimpleFunction, IrSimpleFunction>()

    override fun getFieldForEnumEntry(enumEntry: IrEnumEntry): IrField =
        singletonFieldDeclarations.getOrPut(enumEntry) {
            buildField {
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

    override fun getOuterThisField(innerClass: IrClass): IrField =
        outerThisDeclarations.getOrPut(innerClass) {
            assert(innerClass.isInner) { "Class is not inner: ${innerClass.dump()}" }
            buildField {
                name = Name.identifier("this$0")
                type = innerClass.parentAsClass.defaultType
                origin = DeclarationFactory.FIELD_FOR_OUTER_THIS
                visibility = JavaVisibilities.PACKAGE_VISIBILITY
                isFinal = true
            }.apply {
                parent = innerClass
            }
        }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor {
        val innerClass = innerClassConstructor.parent as IrClass
        assert(innerClass.isInner) { "Class is not inner: ${(innerClassConstructor.parent as IrClass).dump()}" }

        return innerClassConstructors.getOrPut(innerClassConstructor) {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor)
        }.also {
            if (innerClassConstructor.isPrimary) {
                originalInnerClassPrimaryConstructorByClass[innerClass] = innerClassConstructor
            }
        }
    }

    override fun getInnerClassOriginalPrimaryConstructorOrNull(innerClass: IrClass): IrConstructor? {
        assert(innerClass.isInner) { "Class is not inner: $innerClass" }

        return originalInnerClassPrimaryConstructorByClass[innerClass]
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldConstructor: IrConstructor): IrConstructor =
        buildConstructor(oldConstructor.descriptor) {
            updateFrom(oldConstructor)
            returnType = oldConstructor.returnType
        }.apply {
            annotations = oldConstructor.annotations.map { it.deepCopyWithSymbols(this) }
            parent = oldConstructor.parent
            returnType = oldConstructor.returnType
            copyTypeParametersFrom(oldConstructor)

            val outerThisValueParameter = buildValueParameter(this) {
                origin = JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS
                name = Name.identifier(AsmUtil.CAPTURED_THIS_FIELD)
                index = 0
                type = oldConstructor.parentAsClass.parentAsClass.defaultType
            }
            valueParameters = listOf(outerThisValueParameter) + oldConstructor.valueParameters.map { it.copyTo(this, index = it.index + 1) }
            metadata = oldConstructor.metadata
        }

    override fun getFieldForObjectInstance(singleton: IrClass): IrField =
        singletonFieldDeclarations.getOrPut(singleton) {
            val originalVisibility = singleton.visibility
            val isNotMappedCompanion = singleton.isCompanion && !isMappedIntrinsicCompanionObject(singleton.descriptor)
            val useProperVisibilityForCompanion =
                languageVersionSettings.supportsFeature(LanguageFeature.ProperVisibilityForCompanionObjectInstanceField)
                        && singleton.isCompanion
                        && !singleton.parentAsClass.isInterface
            buildField {
                name = if (isNotMappedCompanion) singleton.name else Name.identifier(JvmAbi.INSTANCE_FIELD)
                type = singleton.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                isFinal = true
                isStatic = true
                visibility = when {
                    !useProperVisibilityForCompanion -> Visibilities.PUBLIC
                    originalVisibility == Visibilities.PROTECTED -> JavaVisibilities.PROTECTED_STATIC_VISIBILITY
                    else -> originalVisibility
                }

            }.apply {
                parent = if (isNotMappedCompanion) singleton.parent else singleton
            }
        }

    fun getPrivateFieldForObjectInstance(singleton: IrClass): IrField =
        if (singleton.isCompanion && singleton.parentAsClass.isJvmInterface)
            interfaceCompanionFieldDeclarations.getOrPut(singleton) {
                buildField {
                    name = Name.identifier("\$\$INSTANCE")
                    type = singleton.defaultType
                    origin = JvmLoweredDeclarationOrigin.INTERFACE_COMPANION_PRIVATE_INSTANCE
                    isFinal = true
                    isStatic = true
                    visibility = JavaVisibilities.PACKAGE_VISIBILITY
                }.apply {
                    parent = singleton
                }
            }
        else
            getFieldForObjectInstance(singleton)

    fun getStaticBackingField(irProperty: IrProperty): IrField? {
        // Only fields defined directly in objects should be made static.
        // Fake overrides never point to those, as objects are final.
        if (irProperty.isFakeOverride) return null
        val oldField = irProperty.backingField ?: return null
        val oldParent = irProperty.parent as? IrClass ?: return null
        if (!oldParent.isObject) return null
        return staticBackingFields.getOrPut(irProperty) {
            buildField {
                updateFrom(oldField)
                name = oldField.name
                isStatic = true
            }.apply {
                // We don't move fields to interfaces unless all fields are annotated with @JvmField.
                // It is an error to annotate only some of the fields of an interface companion with
                // @JvmField, so checking the current field only should be enough.
                val hasJvmField = oldField.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
                parent = if (oldParent.isCompanion && (!oldParent.parentAsClass.isJvmInterface || hasJvmField))
                    oldParent.parentAsClass
                else
                    oldParent
                annotations += oldField.annotations
                initializer = oldField.initializer
                    ?.replaceThisByStaticReference(this@JvmDeclarationFactory, oldParent, oldParent.thisReceiver!!)
                    ?.patchDeclarationParents(this) as IrExpressionBody?
                origin = if (irProperty.parentAsClass.isCompanion) JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD else origin
            }
        }
    }

    fun getDefaultImplsFunction(interfaceFun: IrSimpleFunction, forCompatibilityMode: Boolean = false): IrSimpleFunction {
        val parent = interfaceFun.parentAsClass
        assert(parent.isJvmInterface) { "Parent of ${interfaceFun.dump()} should be interface" }
        assert(!forCompatibilityMode || !defaultImplsMethods.containsKey(interfaceFun)) { "DefaultImpls stub in compatibility mode should be requested only once from interface lowering: ${interfaceFun.dump()}" }
        return defaultImplsMethods.getOrPut(interfaceFun) {
            val defaultImpls = getDefaultImplsClass(interfaceFun.parentAsClass)

            val name = Name.identifier(methodSignatureMapper.mapFunctionName(interfaceFun))
            createStaticFunctionWithReceivers(
                defaultImpls, name, interfaceFun,
                dispatchReceiverType = parent.defaultType,
                // If `interfaceFun` is not a real implementation, then we're generating stubs in a descendant
                // interface's DefaultImpls. For example,
                //
                //     interface I1 { fun f() { ... } }
                //     interface I2 : I1
                //
                // is supposed to allow using `I2.DefaultImpls.f` as if it was inherited from `I1.DefaultImpls`.
                // The classes are not actually related and `I2.DefaultImpls.f` is not a fake override but a bridge.
                origin = when {
                    !forCompatibilityMode && !interfaceFun.isFakeOverride -> interfaceFun.origin
                    interfaceFun.resolveFakeOverride()!!.origin.isSynthetic ->
                        if (forCompatibilityMode) JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_FOR_COMPATIBILITY_SYNTHETIC
                        else JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC
                    else ->
                        if (forCompatibilityMode) JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_FOR_COMPATIBILITY
                        else JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE
                },
                // Old backend doesn't generate ACC_FINAL on DefaultImpls methods.
                modality = Modality.OPEN,

                // Interface functions are public or private, with one exception: clone in Cloneable, which is protected.
                // However, Cloneable has no DefaultImpls, so this merely replicates the incorrect behavior of the old backend.
                // We should rather not generate a bridge to clone when interface inherits from Cloneable at all.
                visibility = if (interfaceFun.visibility == Visibilities.PRIVATE) Visibilities.PRIVATE else Visibilities.PUBLIC,

                isFakeOverride = false,
                typeParametersFromContext = parent.typeParameters
            ).also {
                if (it.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_FOR_COMPATIBILITY &&
                    !it.annotations.hasAnnotation(DeprecationResolver.JAVA_DEPRECATED)
                ) {
                    this@JvmDeclarationFactory.context.createIrBuilder(it.symbol).run {
                        it.annotations += irCall(this@JvmDeclarationFactory.context.ir.symbols.javaLangDeprecatedConstructor)
                    }
                }
            }
        }
    }

    fun getDefaultImplsClass(interfaceClass: IrClass): IrClass =
        defaultImplsClasses.getOrPut(interfaceClass) {
            buildClass {
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
            buildFun(fakeOverride.descriptor) {
                origin = JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE
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
                annotations = fakeOverride.annotations
                copyCorrespondingPropertyFrom(fakeOverride)
            }
        }
}
