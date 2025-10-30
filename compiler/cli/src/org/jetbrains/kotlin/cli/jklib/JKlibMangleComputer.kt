/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.error.ErrorClassDescriptor
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.isUnit

/*
 * TODO:
 *  Most of the code in this file is copied from `IrMangleComputer.kt` and `DescriptorMangleComputer.kt` in the Kotlin compiler. This
 *  copying is needed to redefine `mangleTypeArguments` from `BaseKotlinMangleComputer`, so that effective variance is always used when
 *  computing type signature, see `mangleTypeArgumentsUsingEffectiveVariance`. These changes are required to fix differences in type
 *  variance of function arguments computed on the serialization step by K2 from the ones computed during deserialization by K1.
 *  When the K1 klib deserialization part of the K2CL pipeline transitions to K2, code in this file should no longer be needed.
 */

open class JKlibIrMangleComputerBase(
    builder: StringBuilder,
    mode: MangleMode,
    protected val compatibleMode: Boolean,
    allowOutOfScopeTypeParameters: Boolean = false,
) : BaseKotlinMangleComputer<
        /*Declaration=*/IrDeclaration,
        /*Type=*/IrType,
        /*TypeParameter=*/IrTypeParameterSymbol,
        /*ValueParameter=*/IrValueParameter,
        /*TypeParameterContainer=*/IrDeclaration,
        /*FunctionDeclaration=*/IrFunction,
        /*Session=*/Nothing?,
        >(builder, mode, allowOutOfScopeTypeParameters) {

    fun mangleTypeArgumentsUsingEffectiveVariance(tBuilder: StringBuilder, type: IrType, declarationSiteSession: Nothing?) =
        with(getTypeSystemContext(declarationSiteSession)) {
            val typeArguments = type.getArguments().zip(type.typeConstructor().getParameters())
            if (typeArguments.isEmpty()) return
            @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY")
            typeArguments.collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { (typeArgument, typeParameter) ->
                when {
                    typeArgument.isStarProjection() -> appendSignature(MangleConstant.STAR_MARK)
                    else -> {
                        val variance = AbstractTypeChecker.effectiveVariance(
                            typeParameter.getVariance(), typeArgument.getVariance()
                        ) ?: typeArgument.getVariance()
                        if (variance != TypeVariance.INV) {
                            appendSignature(variance.presentation)
                            appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                        }

                        @Suppress("UNCHECKED_CAST")
                        mangleType(this, typeArgument.getType() as IrType, declarationSiteSession)
                    }
                }
            }
        }

    final override fun getTypeSystemContext(session: Nothing?) = object : IrTypeSystemContext {
        override val irBuiltIns: IrBuiltIns
            get() = throw UnsupportedOperationException("Builtins are unavailable")
    }

    override fun copy(newMode: MangleMode) = JKlibIrMangleComputerBase(builder, newMode, compatibleMode)

    final override fun IrDeclaration.visitParent() {
        parent.acceptVoid(Visitor())
    }

    final override fun IrDeclaration.visit() {
        acceptVoid(Visitor())
    }

    override fun IrDeclaration.asTypeParameterContainer(): IrDeclaration =
        this

    override fun IrDeclaration.visitParentForFunctionMangling() {
        val declarationParent = parent
        val realParent = if (declarationParent is IrField && declarationParent.origin == IrDeclarationOrigin.DELEGATE)
            declarationParent.parent
        else
            declarationParent
        realParent.acceptVoid(Visitor())
    }

    override fun getContextParameters(function: IrFunction): List<IrValueParameter> =
        function
            .parameters
            .filter { it.kind == IrParameterKind.Context }
            .filterNot { it.isHidden }

    override fun getExtensionReceiverParameter(function: IrFunction): IrValueParameter? =
        function
            .parameters
            .firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            ?.takeUnless { it.isHidden }

    override fun getRegularParameters(function: IrFunction): List<IrValueParameter> =
        function
            .parameters
            .filter { it.kind == IrParameterKind.Regular }
            .filterNot { it.isHidden }

    override fun getReturnType(function: IrFunction) = function.returnType

    override fun getTypeParametersWithIndices(function: IrFunction, container: IrDeclaration): List<IndexedValue<IrTypeParameterSymbol>> =
        function.typeParameters.map { IndexedValue(it.index, it.symbol) }

    override fun isUnit(type: IrType) = type.isUnit()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    final override fun getEffectiveParent(typeParameter: IrTypeParameterSymbol): IrDeclaration = typeParameter.owner.run {
        when (val irParent = parent) {
            is IrSimpleFunction -> irParent.correspondingPropertySymbol?.owner ?: irParent
            is IrTypeParametersContainer -> irParent
            else -> error("Unexpected type parameter container ${irParent.render()} for TP ${render()}")
        }
    }

    override fun renderDeclaration(declaration: IrDeclaration) = declaration.render()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun getTypeParameterName(typeParameter: IrTypeParameterSymbol) = typeParameter.owner.name.asString()

    final override fun isVararg(valueParameter: IrValueParameter) = valueParameter.varargElementType != null

    final override fun getValueParameterType(valueParameter: IrValueParameter) = valueParameter.type

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    final override fun getIndexOfTypeParameter(typeParameter: IrTypeParameterSymbol, container: IrDeclaration) = typeParameter.owner.index

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    final override fun mangleType(tBuilder: StringBuilder, type: IrType, declarationSiteSession: Nothing?) {
        when (type) {
            is IrSimpleType -> {
                when (val classifier = type.classifier) {
                    is IrClassSymbol -> with(copy(MangleMode.FQNAME)) { classifier.owner.visit() }
                    is IrTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(classifier)
                    is IrScriptSymbol -> {}
                }

                mangleTypeArgumentsUsingEffectiveVariance(tBuilder, type, null)

                //TODO
                if (type.isMarkedNullable()) tBuilder.appendSignature(MangleConstant.Q_MARK)

                mangleTypePlatformSpecific(type, tBuilder)
            }
            is IrDynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is IrErrorType -> tBuilder.appendSignature(MangleConstant.ERROR_MARK)
        }
    }

    private inner class Visitor : IrVisitorVoid() {

        override fun visitElement(element: IrElement) =
            error("unexpected element ${element.render()}")

        override fun visitScript(declaration: IrScript) {
            declaration.visitParent()
        }

        override fun visitClass(declaration: IrClass) {
            typeParameterContainers.add(declaration)

            val className = declaration.name.asString()
            declaration.mangleSimpleDeclaration(className)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment) {
            declaration.packageFqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
        }

        override fun visitProperty(declaration: IrProperty) {
            val accessor = declaration.run { getter ?: setter }
            require(accessor != null || declaration.backingField != null) {
                "Expected at least one accessor or backing field for property ${declaration.render()}"
            }

            typeParameterContainers.add(declaration)
            declaration.visitParent()

            val isStaticProperty = if (accessor != null)
                accessor.let {
                    it.dispatchReceiverParameter == null && declaration.parent !is IrPackageFragment && !declaration.parent.isFacadeClass
                }
            else {
                // Fake override for a Java field
                val backingField = declaration.resolveFakeOverride()?.backingField
                    ?: error("Expected at least one accessor or a backing field for property ${declaration.render()}")
                backingField.isStatic
            }

            if (isStaticProperty) {
                builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
            }

            val contextParameters = accessor?.parameters?.filter { it.kind == IrParameterKind.Context }.orEmpty()
            if (contextParameters.isNotEmpty()) {
                contextParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
                    mangleValueParameter(this, it, null)
                }
            }

            accessor?.parameters?.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.let {
                builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
                mangleValueParameter(builder, it, null)
            }

            val typeParameters = accessor?.typeParameters ?: emptyList()

            typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) {
                mangleTypeParameter(this, it.symbol, it.index, null)
            }

            builder.append(declaration.name.asString())

            if (declaration.isSyntheticForJavaField) {
                builder.append(MangleConstant.JAVA_FIELD_SUFFIX)
            }
        }

        private val IrProperty.isSyntheticForJavaField: Boolean
            get() = origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && getter == null && setter == null

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun visitField(declaration: IrField) {
            val prop = declaration.correspondingPropertySymbol
            if (compatibleMode || prop == null) { // act as used to be (KT-48912)
                // test compiler/testData/codegen/box/ir/serializationRegressions/anonFakeOverride.kt
                declaration.mangleSimpleDeclaration(declaration.name.asString())
            } else {
                visitProperty(prop.owner)
            }
        }

        override fun visitEnumEntry(declaration: IrEnumEntry) {
            declaration.mangleSimpleDeclaration(declaration.name.asString())
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            val klass = declaration.parentAsClass
            val anonInitializers = klass.declarations.filterIsInstance<IrAnonymousInitializer>()

            val anonName = buildString {
                append(MangleConstant.ANON_INIT_NAME_PREFIX)
                if (anonInitializers.size > 1) {
                    append(MangleConstant.LOCAL_DECLARATION_INDEX_PREFIX)
                    append(anonInitializers.indexOf(declaration))
                }
            }

            declaration.mangleSimpleDeclaration(anonName)
        }

        override fun visitTypeAlias(declaration: IrTypeAlias) =
            declaration.mangleSimpleDeclaration(declaration.name.asString())

        override fun visitTypeParameter(declaration: IrTypeParameter) {
            getEffectiveParent(declaration.symbol).visit()

            builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
            builder.appendSignature(declaration.index)
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            val container = declaration.correspondingPropertySymbol?.owner ?: declaration
            val isStatic = declaration.dispatchReceiverParameter == null &&
                    (container.parent !is IrPackageFragment && !container.parent.isFacadeClass)
            declaration.mangleFunction(
                name = declaration.name,
                isConstructor = false,
                isStatic = isStatic,
                container = container,
                session = null
            )
        }

        override fun visitConstructor(declaration: IrConstructor) {
            declaration.mangleFunction(
                name = declaration.name,
                isConstructor = true,
                isStatic = false,
                container = declaration,
                session = null
            )
        }
    }
}

open class JKlibDescriptorMangleComputerBase(builder: StringBuilder, mode: MangleMode) :
    BaseKotlinMangleComputer<
            /*Declaration=*/DeclarationDescriptor,
            /*Type=*/KotlinType,
            /*TypeParameter=*/TypeParameterDescriptor,
            /*ValueParameter=*/ParameterDescriptor,
            /*TypeParameterContainer=*/DeclarationDescriptor, // CallableDescriptor or ClassDescriptor
            /*FunctionDeclaration=*/FunctionDescriptor,
            /*Session=*/Nothing?,
            >(builder, mode) {
    fun mangleTypeArgumentsUsingEffectiveVariance(tBuilder: StringBuilder, type: KotlinType, declarationSiteSession: Nothing?) =
        with(getTypeSystemContext(declarationSiteSession)) {
            val typeArguments = type.getArguments().zip(type.typeConstructor().getParameters())
            if (typeArguments.isEmpty()) return
            @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY")
            typeArguments.collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { (typeArgument, typeParameter) ->
                when {
                    typeArgument.isStarProjection() -> appendSignature(MangleConstant.STAR_MARK)
                    else -> {
                        val variance = AbstractTypeChecker.effectiveVariance(
                            typeParameter.getVariance(), typeArgument.getVariance()
                        ) ?: typeArgument.getVariance()
                        if (variance != TypeVariance.INV) {
                            appendSignature(variance.presentation)
                            appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                        }

                        @Suppress("UNCHECKED_CAST")
                        mangleType(this, typeArgument.getType() as KotlinType, declarationSiteSession)
                    }
                }
            }
        }

    final override fun getTypeSystemContext(session: Nothing?): TypeSystemContext = SimpleClassicTypeSystemContext

    override fun copy(newMode: MangleMode) = JKlibDescriptorMangleComputerBase(builder, newMode)

    final override fun DeclarationDescriptor.visitParent() {
        containingDeclaration?.visit()
    }

    final override fun DeclarationDescriptor.visit() {
        accept(Visitor(), null)
    }

    private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
        error("unexpected descriptor $descriptor")
    }

    open fun PropertyDescriptor.platformSpecificSuffix(): String? = null

    private val CallableDescriptor.isRealStatic: Boolean
        get() = dispatchReceiverParameter == null && containingDeclaration !is PackageFragmentDescriptor

    override fun DeclarationDescriptor.asTypeParameterContainer(): DeclarationDescriptor =
        this

    override fun getContextParameters(function: FunctionDescriptor): List<ParameterDescriptor> =
        function.contextReceiverParameters

    override fun getExtensionReceiverParameter(function: FunctionDescriptor): ParameterDescriptor? =
        function.extensionReceiverParameter

    override fun getRegularParameters(function: FunctionDescriptor): List<ValueParameterDescriptor> =
        function.valueParameters

    override fun getReturnType(function: FunctionDescriptor): KotlinType? =
        function.returnType

    override fun getTypeParametersWithIndices(
        function: FunctionDescriptor,
        container: DeclarationDescriptor,
    ): List<IndexedValue<TypeParameterDescriptor>> =
        (container as? CallableDescriptor)
            ?.typeParameters
            .orEmpty()
            .filter { it.containingDeclaration == container }
            .map { IndexedValue(it.index, it) }

    override fun isUnit(type: KotlinType) = type.isUnit()

    final override fun isVararg(valueParameter: ParameterDescriptor) = valueParameter.varargElementType != null

    final override fun getValueParameterType(valueParameter: ParameterDescriptor): KotlinType =
        valueParameter.type

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    final override fun mangleType(tBuilder: StringBuilder, wrappedType: KotlinType, declarationSiteSession: Nothing?) {
        when (val type = wrappedType.unwrap()) {
            is SimpleType -> {

                when (val classifier = type.constructor.declarationDescriptor) {
                    is ErrorClassDescriptor -> {
                        tBuilder.appendSignature(MangleConstant.ERROR_MARK)
                        return
                    }
                    is ClassDescriptor -> with(copy(MangleMode.FQNAME)) { classifier.visit() }
                    is TypeParameterDescriptor -> tBuilder.mangleTypeParameterReference(classifier)
                    else -> error("Unexpected classifier: $classifier")
                }

                mangleTypeArgumentsUsingEffectiveVariance(tBuilder, type, null)

                if (type.isMarkedNullable) tBuilder.appendSignature(MangleConstant.Q_MARK)

                mangleTypePlatformSpecific(type, tBuilder)
            }
            is DynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is FlexibleType -> {
                // Reproduce type approximation done for flexible types in TypeTranslator.
                val upper = type.upperBound
                val upperDescriptor = upper.constructor.declarationDescriptor
                    ?: error("No descriptor for type $upper")
                if (upperDescriptor is ClassDescriptor) {
                    val lower = type.lowerBound
                    val lowerDescriptor = lower.constructor.declarationDescriptor as? ClassDescriptor
                        ?: error("No class descriptor for lower type $lower of $type")
                    val intermediate = if (lowerDescriptor == upperDescriptor && type !is RawType) {
                        lower.replace(newArguments = upper.arguments)
                    } else lower
                    val mixed = intermediate.makeNullableAsSpecified(upper.isMarkedNullable)
                    mangleType(tBuilder, mixed, null)
                } else mangleType(tBuilder, upper, null)
            }
        }
    }

    final override fun getEffectiveParent(typeParameter: TypeParameterDescriptor) = typeParameter.containingDeclaration

    override fun renderDeclaration(declaration: DeclarationDescriptor) = DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(declaration)

    override fun getTypeParameterName(typeParameter: TypeParameterDescriptor) = typeParameter.name.asString()

    final override fun getIndexOfTypeParameter(typeParameter: TypeParameterDescriptor, container: DeclarationDescriptor) =
        typeParameter.index

    private fun manglePropertyAccessor(accessor: PropertyAccessorDescriptor) {
        val property = accessor.correspondingProperty
        accessor.mangleFunction(
            name = accessor.name,
            isConstructor = false,
            isStatic = property.isRealStatic,
            container = property,
            session = null
        )
    }

    protected open fun visitModuleDeclaration(descriptor: ModuleDescriptor) = reportUnexpectedDescriptor(descriptor)

    private inner class Visitor : DeclarationDescriptorVisitor<Unit, Nothing?> {

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Nothing?) {
            descriptor.fqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Nothing?) = reportUnexpectedDescriptor(descriptor)

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Nothing?) = reportUnexpectedDescriptor(descriptor)

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Nothing?) {
            descriptor.mangleFunction(
                name = descriptor.name,
                isConstructor = false,
                isStatic = descriptor.isRealStatic,
                container = descriptor,
                session = null
            )
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Nothing?) {
            descriptor.containingDeclaration.visit()

            builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
            builder.appendSignature(descriptor.index)
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Nothing?) {
            typeParameterContainers.add(descriptor)
            descriptor.mangleSimpleDeclaration(descriptor.name.asString())
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Nothing?) {
            descriptor.mangleSimpleDeclaration(descriptor.name.asString())
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Nothing?) {
            visitModuleDeclaration(descriptor)
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: Nothing?) {
            constructorDescriptor.mangleFunction(
                name = constructorDescriptor.name,
                isConstructor = true,
                isStatic = constructorDescriptor.isRealStatic,
                container = constructorDescriptor,
                session = null
            )
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: Nothing?) =
            visitClassDescriptor(scriptDescriptor, data)

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Nothing?) {

            if (descriptor is IrImplementingDelegateDescriptor) {
                descriptor.mangleSimpleDeclaration(descriptor.name.asString())
            } else {
                val actualDescriptor = (descriptor as? IrPropertyDelegateDescriptor)?.correspondingProperty ?: descriptor

                typeParameterContainers.add(actualDescriptor)
                actualDescriptor.containingDeclaration.visit()

                if (actualDescriptor.isRealStatic) {
                    builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
                }

                val contextParameters = actualDescriptor.contextReceiverParameters
                if (contextParameters.isNotEmpty()) {
                    contextParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
                        mangleValueParameter(this, it, null)
                    }
                }

                val extensionReceiver = actualDescriptor.extensionReceiverParameter
                if (extensionReceiver != null) {
                    builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
                    mangleValueParameter(builder, extensionReceiver, null)
                }

                actualDescriptor.typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) {
                    mangleTypeParameter(this, it, it.index, null)
                }

                builder.append(actualDescriptor.name.asString())

                actualDescriptor.platformSpecificSuffix()?.let {
                    builder.appendSignature(it)
                }
            }
        }

        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Nothing?) =
            reportUnexpectedDescriptor(descriptor)

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Nothing?) {
            manglePropertyAccessor(descriptor)
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Nothing?) {
            manglePropertyAccessor(descriptor)
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Nothing?) =
            reportUnexpectedDescriptor(descriptor)
    }
}
