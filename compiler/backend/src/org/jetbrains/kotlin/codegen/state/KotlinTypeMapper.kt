/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.isStaticMethod
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.*
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.coroutines.isSuspendFunctionNotSuspensionView
import org.jetbrains.kotlin.codegen.coroutines.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass
import org.jetbrains.kotlin.codegen.coroutines.unwrapInitialDescriptorForSuspendFunction
import org.jetbrains.kotlin.codegen.inline.FictitiousArrayConstructor
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForDeserialized
import org.jetbrains.kotlin.load.java.getJvmMethodNameIfSpecial
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider.IncrementalMultifileClassPackageFragment
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall
import org.jetbrains.kotlin.resolve.BindingContextUtils.isBoxedLocalCapturedInClosure
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.DEFAULT_CONSTRUCTOR_MARKER
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.annotations.isCompiledToJvmDefault
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import kotlin.collections.*

class KotlinTypeMapper @JvmOverloads constructor(
    val bindingContext: BindingContext,
    val classBuilderMode: ClassBuilderMode,
    private val moduleName: String,
    val languageVersionSettings: LanguageVersionSettings,
    private val useOldInlineClassesManglingScheme: Boolean,
    val jvmTarget: JvmTarget = JvmTarget.DEFAULT,
    private val isIrBackend: Boolean = false,
    private val typePreprocessor: ((KotlinType) -> KotlinType?)? = null,
    private val namePreprocessor: ((ClassDescriptor) -> String?)? = null
) : KotlinTypeMapperBase() {
    val jvmDefaultMode = languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)
    var useOldManglingRulesForFunctionAcceptingInlineClass: Boolean = useOldInlineClassesManglingScheme
        set(value) {
            require(!useOldInlineClassesManglingScheme)
            field = value
        }

    override val typeSystem: TypeSystemCommonBackendContext
        get() = SimpleClassicTypeSystemContext

    private val typeMappingConfiguration = object : TypeMappingConfiguration<Type> {
        override fun commonSupertype(types: Collection<KotlinType>): KotlinType {
            return CommonSupertypes.commonSupertype(types)
        }

        override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): Type? {
            return bindingContext.get(ASM_TYPE, classDescriptor)
        }

        override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? {
            return getPredefinedTypeForClass(classDescriptor)?.internalName
        }

        override fun getPredefinedFullInternalNameForClass(classDescriptor: ClassDescriptor): String? {
            return namePreprocessor?.invoke(classDescriptor)
        }

        override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
            if (classBuilderMode.generateBodies) {
                throw IllegalStateException(generateErrorMessageForErrorType(kotlinType, descriptor))
            }
        }

        override fun preprocessType(kotlinType: KotlinType): KotlinType? {
            return typePreprocessor?.invoke(kotlinType)
        }
    }

    fun mapOwner(descriptor: DeclarationDescriptor): Type {
        return mapOwner(descriptor, true)
    }

    fun mapImplementationOwner(descriptor: DeclarationDescriptor): Type {
        return mapOwner(descriptor, false)
    }

    private fun mapOwner(descriptor: DeclarationDescriptor, publicFacade: Boolean): Type {
        if (isLocalFunction(descriptor)) {
            return asmTypeForAnonymousClass(
                bindingContext,
                (descriptor as FunctionDescriptor).unwrapInitialDescriptorForSuspendFunction()
            )
        }

        if (descriptor is ConstructorDescriptor) {
            return mapClass(descriptor.constructedClass)
        }

        return when (val container = descriptor.containingDeclaration) {
            is PackageFragmentDescriptor -> {
                val packageMemberOwner = internalNameForPackageMemberOwner(descriptor as CallableMemberDescriptor, publicFacade)
                Type.getObjectType(packageMemberOwner)
            }
            is ClassDescriptor -> mapClass((container as ClassDescriptor?)!!)
            else -> throw UnsupportedOperationException("Don't know how to map owner for $descriptor")
        }
    }

    class ContainingClassesInfo(val facadeClassId: ClassId, val implClassId: ClassId) {

        companion object {
            internal fun forPackageMember(
                facadeName: JvmClassName,
                partName: JvmClassName
            ): ContainingClassesInfo {
                return ContainingClassesInfo(
                    ClassId.topLevel(facadeName.fqNameForTopLevelClassMaybeWithDollars),
                    ClassId.topLevel(partName.fqNameForTopLevelClassMaybeWithDollars)
                )
            }

            internal fun forClassMember(classId: ClassId): ContainingClassesInfo {
                return ContainingClassesInfo(classId, classId)
            }
        }
    }

    @JvmOverloads
    fun mapReturnType(descriptor: CallableDescriptor, sw: JvmSignatureWriter? = null): Type {
        val returnType = descriptor.returnType ?: error("Function has no return type: $descriptor")

        if (descriptor is ConstructorDescriptor) {
            return Type.VOID_TYPE
        }

        if (descriptor.isSuspendFunctionNotSuspensionView()) {
            return mapReturnType(getOrCreateJvmSuspendFunctionView(descriptor as SimpleFunctionDescriptor), sw)
        }

        if (hasVoidReturnType(descriptor)) {
            sw?.writeAsmType(Type.VOID_TYPE)
            return Type.VOID_TYPE
        } else if (descriptor is FunctionDescriptor && forceBoxedReturnType(descriptor)) {
            return mapType(descriptor.getReturnType()!!, sw, TypeMappingMode.RETURN_TYPE_BOXED)
        }

        return mapReturnType(descriptor, sw, returnType)
    }

    private fun mapReturnType(descriptor: CallableDescriptor, sw: JvmSignatureWriter?, returnType: KotlinType): Type {
        val isAnnotationMethod = isAnnotationClass(descriptor.containingDeclaration)
        if (sw == null || sw.skipGenericSignature()) {
            return mapType(returnType, sw, TypeMappingMode.getModeForReturnTypeNoGeneric(isAnnotationMethod))
        }

        val typeMappingModeFromAnnotation =
            extractTypeMappingModeFromAnnotation(descriptor, returnType, isAnnotationMethod, mapTypeAliases = false)
        if (typeMappingModeFromAnnotation != null) {
            return mapType(returnType, sw, typeMappingModeFromAnnotation)
        }

        val mappingMode = typeSystem.getOptimalModeForReturnType(returnType, isAnnotationMethod)

        return mapType(returnType, sw, mappingMode)
    }

    fun mapSupertype(type: KotlinType, signatureVisitor: JvmSignatureWriter?): Type {
        return mapType(type, signatureVisitor, TypeMappingMode.SUPER_TYPE)
    }

    fun mapTypeArgument(type: KotlinType, signatureVisitor: JvmSignatureWriter?): Type {
        return mapType(type, signatureVisitor, TypeMappingMode.GENERIC_ARGUMENT)
    }

    override fun mapClass(classifier: ClassifierDescriptor): Type {
        return mapType(classifier.defaultType, null, TypeMappingMode.CLASS_DECLARATION)
    }

    override fun mapTypeCommon(type: KotlinTypeMarker, mode: TypeMappingMode): Type {
        return mapType(type as KotlinType, null, mode)
    }

    fun mapTypeAsDeclaration(kotlinType: KotlinType): Type {
        return mapType(kotlinType, null, TypeMappingMode.CLASS_DECLARATION)
    }

    fun mapType(descriptor: CallableDescriptor): Type {
        return mapType(descriptor.returnType!!)
    }

    fun mapTypeAsDeclaration(descriptor: CallableDescriptor): Type {
        return mapTypeAsDeclaration(descriptor.returnType!!)
    }

    fun mapAnnotationParameterSignature(descriptor: PropertyDescriptor): JvmMethodGenericSignature {
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.METHOD)
        sw.writeReturnType()
        mapType(descriptor.type, sw, TypeMappingMode.VALUE_FOR_ANNOTATION)
        sw.writeReturnTypeEnd()
        return sw.makeJvmMethodSignature(mapAnnotationParameterName(descriptor))
    }

    fun mapAnnotationParameterName(descriptor: PropertyDescriptor): String {
        val getter = descriptor.getter
        return if (getter != null) mapFunctionName(getter, OwnerKind.IMPLEMENTATION) else descriptor.name.asString()
    }

    fun mapType(descriptor: ClassifierDescriptor): Type {
        return mapType(descriptor.defaultType)
    }

    @JvmOverloads
    fun mapType(
        type: KotlinType,
        signatureVisitor: JvmSignatureWriter? = null,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT
    ): Type {
        if (isIrBackend) {
            throw AssertionError("IR backend shouldn't call KotlinTypeMapper.mapType: $type")
        }

        return mapType(
            type, AsmTypeFactory, mode, typeMappingConfiguration, signatureVisitor
        ) { ktType, asmType, typeMappingMode ->
            writeGenericType(ktType, asmType, signatureVisitor, typeMappingMode)
        }
    }

    private fun writeGenericType(
        type: KotlinType,
        asmType: Type,
        signatureVisitor: JvmSignatureWriter?,
        mode: TypeMappingMode
    ) {
        if (signatureVisitor == null) return

        // Nothing mapping rules:
        //  Map<Nothing, Foo> -> Map
        //  Map<Foo, List<Nothing>> -> Map<Foo, List>
        //  In<Nothing, Foo> == In<*, Foo> -> In<?, Foo>
        //  In<Nothing, Nothing> -> In
        //  Inv<in Nothing, Foo> -> Inv
        if (signatureVisitor.skipGenericSignature() || hasNothingInNonContravariantPosition(type) || type.arguments.isEmpty()) {
            signatureVisitor.writeAsmType(asmType)
            return
        }

        val possiblyInnerType = type.buildPossiblyInnerType() ?: error("possiblyInnerType with arguments should not be null")

        val innerTypesAsList = possiblyInnerType.segments()

        val indexOfParameterizedType = innerTypesAsList.indexOfFirst { innerPart -> innerPart.arguments.isNotEmpty() }
        if (indexOfParameterizedType < 0 || innerTypesAsList.size == 1) {
            signatureVisitor.writeClassBegin(asmType)
            writeGenericArguments(signatureVisitor, possiblyInnerType, mode)
        } else {
            val outerType = innerTypesAsList[indexOfParameterizedType]

            signatureVisitor.writeOuterClassBegin(asmType, mapType(outerType.classDescriptor).internalName)
            writeGenericArguments(signatureVisitor, outerType, mode)

            writeInnerParts(innerTypesAsList, signatureVisitor, mode, indexOfParameterizedType + 1) // inner parts separated by `.`
        }

        signatureVisitor.writeClassEnd()
    }

    private fun writeInnerParts(
        innerTypesAsList: List<PossiblyInnerType>,
        signatureVisitor: JvmSignatureWriter,
        mode: TypeMappingMode,
        index: Int
    ) {
        for (innerPart in innerTypesAsList.subList(index, innerTypesAsList.size)) {
            signatureVisitor.writeInnerClass(getJvmShortName(innerPart.classDescriptor))
            writeGenericArguments(signatureVisitor, innerPart, mode)
        }
    }

    private fun writeGenericArguments(
        signatureVisitor: JvmSignatureWriter,
        type: PossiblyInnerType,
        mode: TypeMappingMode
    ) {
        val classDescriptor = type.classDescriptor
        val parameters = classDescriptor.declaredTypeParameters
        val arguments = type.arguments

        if (classDescriptor is FunctionClassDescriptor) {
            if (classDescriptor.hasBigArity ||
                classDescriptor.functionKind == FunctionClassKind.KFunction ||
                classDescriptor.functionKind == FunctionClassKind.KSuspendFunction
            ) {
                // kotlin.reflect.KFunction{n}<P1, ..., Pn, R> is mapped to kotlin.reflect.KFunction<R> (for all n), and
                // kotlin.Function{n}<P1, ..., Pn, R> is mapped to kotlin.jvm.functions.FunctionN<R> (for n > 22).
                // So for these classes, we need to skip all type arguments except the very last one
                writeGenericArguments(signatureVisitor, listOf(arguments.last()), listOf(parameters.last()), mode)
                return
            }
        }

        writeGenericArguments(signatureVisitor, arguments, parameters, mode)
    }

    private fun writeGenericArguments(
        signatureVisitor: JvmSignatureWriter,
        arguments: List<TypeProjection>,
        parameters: List<TypeParameterDescriptor>,
        mode: TypeMappingMode
    ) {
        with(SimpleClassicTypeSystemContext) {
            writeGenericArguments(signatureVisitor, arguments, parameters, mode) { type, sw, mode ->
                mapType(type as KotlinType, sw, mode)
            }
        }
    }

    @JvmOverloads
    fun mapToCallableMethod(
        descriptor: FunctionDescriptor,
        superCall: Boolean,
        kind: OwnerKind? = null,
        resolvedCall: ResolvedCall<*>? = null
    ): CallableMethod {
        fun mapDefaultCallback(descriptor: FunctionDescriptor, kind: OwnerKind): () -> Method {
            if (useOldManglingRulesForFunctionAcceptingInlineClass && !useOldInlineClassesManglingScheme) {
                return {
                    val prevManglingState = useOldManglingRulesForFunctionAcceptingInlineClass
                    useOldManglingRulesForFunctionAcceptingInlineClass = true
                    mapDefaultMethod(descriptor, kind).also {
                        useOldManglingRulesForFunctionAcceptingInlineClass = prevManglingState
                    }
                }
            } else {
                return { mapDefaultMethod(descriptor, kind) }
            }
        }

        // we generate constructors of inline classes as usual functions
        if (descriptor is ConstructorDescriptor && kind != OwnerKind.ERASED_INLINE_CLASS) {
            val method = mapSignatureSkipGeneric(descriptor.original)
            val owner = mapOwner(descriptor)
            val originalDescriptor = descriptor.original
            return CallableMethod(
                owner, owner, mapDefaultCallback(originalDescriptor, OwnerKind.IMPLEMENTATION), method, INVOKESPECIAL,
                null, null, null, null, null, originalDescriptor.returnType, isInterfaceMethod = false, isDefaultMethodInInterface = false,
                boxInlineClassBeforeInvoke = false
            )
        }

        if (descriptor is LocalVariableAccessorDescriptor) {
            val delegateAccessorResolvedCall = bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, descriptor)
            return mapToCallableMethod(delegateAccessorResolvedCall!!.resultingDescriptor, false)
        }

        val functionParent = descriptor.original.containingDeclaration

        var functionDescriptor = findSuperDeclaration(descriptor.original, superCall, jvmDefaultMode)

        val signature: JvmMethodSignature
        val returnKotlinType: KotlinType?
        val owner: Type
        val ownerForDefaultImpl: Type
        val baseMethodDescriptor: FunctionDescriptor
        val invokeOpcode: Int
        val thisClass: Type?
        val dispatchReceiverKotlinType: KotlinType?
        var isInterfaceMember = false
        var isDefaultMethodInInterface = false
        var boxInlineClassBeforeInvoke = false

        if (functionParent is ClassDescriptor) {
            val declarationFunctionDescriptor = findAnyDeclaration(functionDescriptor)

            val declarationOwner = declarationFunctionDescriptor.containingDeclaration as ClassDescriptor

            val originalIsInterface = isJvmInterface(declarationOwner)
            val currentIsInterface = isJvmInterface(functionParent)

            val isInterface = currentIsInterface && originalIsInterface

            baseMethodDescriptor = findBaseDeclaration(functionDescriptor).original
            val ownerForDefault = baseMethodDescriptor.containingDeclaration as ClassDescriptor
            isDefaultMethodInInterface = isJvmInterface(ownerForDefault) && baseMethodDescriptor.isCompiledToJvmDefault(jvmDefaultMode)
            ownerForDefaultImpl = if (isJvmInterface(ownerForDefault) && !baseMethodDescriptor.isCompiledToJvmDefault(jvmDefaultMode))
                mapDefaultImpls(ownerForDefault)
            else
                mapClass(ownerForDefault)

            if (isInterface && (superCall || descriptor.visibility == DescriptorVisibilities.PRIVATE || isAccessor(descriptor))) {
                thisClass = mapClass(functionParent)
                dispatchReceiverKotlinType = functionParent.defaultType
                if (declarationOwner is JavaClassDescriptor ||
                    (declarationFunctionDescriptor.isCompiledToJvmDefault(jvmDefaultMode) && !isAccessor(descriptor))
                ) {
                    invokeOpcode = INVOKESPECIAL
                    signature = mapSignatureSkipGeneric(functionDescriptor)
                    returnKotlinType = functionDescriptor.returnType
                    owner = thisClass
                    isInterfaceMember = true
                } else {
                    invokeOpcode = INVOKESTATIC
                    val originalDescriptor = descriptor.original
                    signature = mapSignatureSkipGeneric(originalDescriptor, OwnerKind.DEFAULT_IMPLS)
                    returnKotlinType = originalDescriptor.returnType
                    if (descriptor is AccessorForCallableDescriptor<*> && descriptor.calleeDescriptor.isCompiledToJvmDefault(jvmDefaultMode)) {
                        owner = mapClass(functionParent)
                        isInterfaceMember = true
                    } else {
                        owner = mapDefaultImpls(functionParent)
                    }
                }
            } else {
                val toInlinedErasedClass = functionParent.isInlineClass() &&
                        (!isAccessor(functionDescriptor) || isInlineClassConstructorAccessor(functionDescriptor))

                if (toInlinedErasedClass) {
                    functionDescriptor = descriptor
                }

                val isFakeOverrideOfJvmDefault = toInlinedErasedClass &&
                        functionDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
                        functionDescriptor.overridesJvmDefault()

                val isStaticInvocation = !isFakeOverrideOfJvmDefault &&
                        (isStaticDeclaration(functionDescriptor) && functionDescriptor !is ImportedFromObjectCallableDescriptor<*> ||
                                isStaticAccessor(functionDescriptor) ||
                                functionDescriptor.isJvmStaticInObjectOrClassOrInterface() ||
                                toInlinedErasedClass)
                when {
                    isStaticInvocation -> {
                        invokeOpcode = INVOKESTATIC
                        isInterfaceMember = currentIsInterface && functionParent is JavaClassDescriptor
                    }
                    isInterface -> {
                        invokeOpcode = INVOKEINTERFACE
                        isInterfaceMember = true
                    }
                    isFakeOverrideOfJvmDefault -> {
                        invokeOpcode = INVOKEVIRTUAL
                        boxInlineClassBeforeInvoke = true
                    }
                    else -> {
                        invokeOpcode =
                            if (superCall || DescriptorVisibilities.isPrivate(functionDescriptor.visibility)) INVOKESPECIAL else INVOKEVIRTUAL
                        isInterfaceMember = false
                    }
                }

                val overriddenSpecialBuiltinFunction = functionDescriptor.original.getOverriddenBuiltinReflectingJvmDescriptor()
                val functionToCall = if (overriddenSpecialBuiltinFunction != null && !superCall && !toInlinedErasedClass)
                    overriddenSpecialBuiltinFunction.original
                else
                    functionDescriptor.original

                signature = if (toInlinedErasedClass && !isFakeOverrideOfJvmDefault)
                    mapSignatureForInlineErasedClassSkipGeneric(functionToCall)
                else
                    mapSignature(
                        functionToCall, resolvedCall, OwnerKind.IMPLEMENTATION,
                        skipGenericSignature = true,
                        hasSpecialBridge = false
                    )

                returnKotlinType =
                    (if (functionToCall.isSuspend &&
                        functionToCall.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass(this) == null
                    ) functionDescriptor.builtIns.nullableAnyType else functionToCall.returnType)

                val receiver = if (currentIsInterface && !originalIsInterface || functionParent is FunctionClassDescriptor)
                    declarationOwner
                else
                    functionParent
                owner = mapClass(receiver)
                thisClass = owner
                dispatchReceiverKotlinType = receiver.defaultType
            }
        } else {
            val originalDescriptor = functionDescriptor.original
            signature = mapSignatureSkipGeneric(originalDescriptor)
            returnKotlinType = originalDescriptor.returnType
            owner = mapOwner(functionDescriptor)
            ownerForDefaultImpl = owner
            baseMethodDescriptor = functionDescriptor
            when {
                functionParent is PackageFragmentDescriptor -> {
                    invokeOpcode = INVOKESTATIC
                    thisClass = null
                    dispatchReceiverKotlinType = null
                }
                functionDescriptor is ConstructorDescriptor -> {
                    invokeOpcode = INVOKESPECIAL
                    thisClass = null
                    dispatchReceiverKotlinType = null
                }
                else -> {
                    invokeOpcode = INVOKEVIRTUAL
                    thisClass = owner
                    val ownerDescriptor = functionDescriptor.containingDeclaration
                    dispatchReceiverKotlinType = (ownerDescriptor as? ClassDescriptor)?.defaultType
                }
            }
        }

        val calleeType = if (isLocalFunction(functionDescriptor)) owner else null

        val receiverParameterType: Type?
        val extensionReceiverKotlinType: KotlinType?
        val receiverParameter = functionDescriptor.original.extensionReceiverParameter
        if (receiverParameter != null) {
            extensionReceiverKotlinType = receiverParameter.type
            receiverParameterType = mapType(extensionReceiverKotlinType)
        } else {
            extensionReceiverKotlinType = null
            receiverParameterType = null
        }

        return CallableMethod(
            owner, ownerForDefaultImpl,
            mapDefaultCallback(baseMethodDescriptor, getKindForDefaultImplCall(baseMethodDescriptor)),
            signature, invokeOpcode, thisClass, dispatchReceiverKotlinType, receiverParameterType, extensionReceiverKotlinType,
            calleeType, returnKotlinType,
            if (jvmTarget >= JvmTarget.JVM_1_8) isInterfaceMember else invokeOpcode == INVOKEINTERFACE,
            isDefaultMethodInInterface, boxInlineClassBeforeInvoke
        )
    }

    private fun CallableMemberDescriptor.overridesJvmDefault(): Boolean {
        if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return overriddenDescriptors.any { it.overridesJvmDefault() }
        }
        if (isCompiledToJvmDefault(jvmDefaultMode)) return true
        return (containingDeclaration as? JavaClassDescriptor)?.kind == ClassKind.INTERFACE && modality != Modality.ABSTRACT
    }

    fun mapFunctionName(descriptor: FunctionDescriptor, kind: OwnerKind?): String {
        if (descriptor !is JavaCallableMemberDescriptor) {
            val platformName = getJvmName(descriptor)
            if (platformName != null) return platformName
        }

        val nameForSpecialFunction = getJvmMethodNameIfSpecial(descriptor)
        if (nameForSpecialFunction != null) return nameForSpecialFunction

        return when {
            descriptor is PropertyAccessorDescriptor -> {
                val property = descriptor.correspondingProperty
                val containingDeclaration = property.containingDeclaration

                if (isAnnotationClass(containingDeclaration) &&
                    (!property.hasJvmStaticAnnotation() && !descriptor.hasJvmStaticAnnotation())
                ) {
                    return property.name.asString()
                }

                if ((containingDeclaration as? ClassDescriptor)?.hasJavaLangRecordSupertype() == true) return property.name.asString()

                val isAccessor = property is AccessorForPropertyDescriptor
                val propertyName = if (isAccessor)
                    (property as AccessorForPropertyDescriptor).accessorSuffix
                else
                    property.name.asString()

                val accessorName = if (descriptor is PropertyGetterDescriptor)
                    JvmAbi.getterName(propertyName)
                else
                    JvmAbi.setterName(propertyName)

                mangleMemberNameIfRequired(if (isAccessor) "access$$accessorName" else accessorName, descriptor, kind)
            }
            isFunctionLiteral(descriptor) -> {
                val element = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)
                if (element is KtFunctionLiteral) {
                    val expression = element.parent
                    if (expression is KtLambdaExpression) {
                        val samType = bindingContext.get(SAM_VALUE, expression as KtExpression)
                        if (samType != null) {
                            return samType.originalAbstractMethod.name.asString()
                        }
                    }
                }

                OperatorNameConventions.INVOKE.asString()
            }
            isLocalFunction(descriptor) || isFunctionExpression(descriptor) ->
                OperatorNameConventions.INVOKE.asString()
            else ->
                mangleMemberNameIfRequired(descriptor.name.asString(), descriptor, kind)
        }
    }

    private fun ClassDescriptor.hasJavaLangRecordSupertype() =
        typeConstructor.supertypes.any { KotlinBuiltIns.isConstructedFromGivenClass(it, JAVA_LANG_RECORD_FQ_NAME) }

    private val shouldMangleByReturnType =
        languageVersionSettings.supportsFeature(LanguageFeature.MangleClassMembersReturningInlineClasses)

    private fun mangleMemberNameIfRequired(
        name: String,
        descriptor: CallableMemberDescriptor,
        kind: OwnerKind?
    ): String {
        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration is ScriptDescriptor && descriptor is PropertyDescriptor) {
            //script properties should be public
            return name
        }

        // Special methods for inline classes.
        if (InlineClassDescriptorResolver.isSynthesizedBoxMethod(descriptor)) {
            return BOX_JVM_METHOD_NAME
        }
        if (InlineClassDescriptorResolver.isSynthesizedUnboxMethod(descriptor)) {
            return UNBOX_JVM_METHOD_NAME
        }
        if (InlineClassDescriptorResolver.isSpecializedEqualsMethod(descriptor)) {
            return name
        }

        var newName = name
        // Constructor:
        //   either a constructor method for inline class (should be mangled),
        //   or should stay as it is ('<init>').
        if (descriptor is ConstructorDescriptor) {
            if (kind === OwnerKind.ERASED_INLINE_CLASS) {
                newName = JvmAbi.ERASED_INLINE_CONSTRUCTOR_NAME
            } else {
                return name
            }
        }

        // Skip inline class mangling for property reference signatures,
        // so that we don't have to repeat the same logic in reflection
        // in case of properties without getter methods.
        if (kind !== OwnerKind.PROPERTY_REFERENCE_SIGNATURE || descriptor.isPropertyWithGetterSignaturePresent()) {
            val suffix = getManglingSuffixBasedOnKotlinSignature(
                descriptor,
                shouldMangleByReturnType,
                useOldManglingRulesForFunctionAcceptingInlineClass
            )
            if (suffix != null) {
                newName += suffix
            } else if (kind === OwnerKind.ERASED_INLINE_CLASS) {
                newName += JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS
            }
        }

        newName = sanitizeNameIfNeeded(newName, languageVersionSettings)

        if (isTopLevelDeclaration(descriptor)) {
            if (DescriptorVisibilities.isPrivate(descriptor.visibility) && descriptor !is ConstructorDescriptor && "<clinit>" != newName) {
                val partName = getPartSimpleNameForMangling(descriptor)
                if (partName != null) return "$newName$$partName"
            }
            return newName
        }

        return if (descriptor !is ConstructorDescriptor &&
            descriptor.visibility === DescriptorVisibilities.INTERNAL &&
            !descriptor.isPublishedApi()
        ) {
            InternalNameMapper.mangleInternalName(newName, getModuleName(descriptor))
        } else newName
    }


    private fun CallableMemberDescriptor.isPropertyWithGetterSignaturePresent(): Boolean {
        val propertyDescriptor = when (this) {
            is PropertyDescriptor -> this
            is PropertyAccessorDescriptor -> correspondingProperty
            else -> return false
        }
        return PropertyCodegen.isReferenceablePropertyWithGetter(propertyDescriptor)
    }

    private fun getModuleName(descriptor: CallableMemberDescriptor): String {
        return getJvmModuleNameForDeserializedDescriptor(descriptor) ?: moduleName
    }

    fun mapAsmMethod(descriptor: FunctionDescriptor): Method {
        return mapSignature(descriptor).asmMethod
    }

    fun mapPropertyReferenceSignature(descriptor: FunctionDescriptor): Method {
        return mapSignature(descriptor, OwnerKind.PROPERTY_REFERENCE_SIGNATURE, true).asmMethod
    }

    fun mapAsmMethod(descriptor: FunctionDescriptor, kind: OwnerKind): Method {
        return mapSignature(descriptor, kind, true).asmMethod
    }

    private fun mapSignature(f: FunctionDescriptor): JvmMethodGenericSignature {
        return mapSignature(f, OwnerKind.IMPLEMENTATION, true)
    }

    fun mapSignatureForInlineErasedClassSkipGeneric(f: FunctionDescriptor): JvmMethodSignature {
        return mapSignatureSkipGeneric(f, OwnerKind.ERASED_INLINE_CLASS)
    }

    fun mapSignatureForBoxMethodOfInlineClass(f: FunctionDescriptor): JvmMethodGenericSignature {
        return mapSignature(f, OwnerKind.IMPLEMENTATION, true)
    }

    fun mapSignatureForSpecializedEqualsOfInlineClass(f: FunctionDescriptor): JvmMethodGenericSignature {
        return mapSignature(f, OwnerKind.IMPLEMENTATION, true)
    }

    @JvmOverloads
    fun mapSignatureSkipGeneric(f: FunctionDescriptor, kind: OwnerKind = OwnerKind.IMPLEMENTATION): JvmMethodSignature {
        return mapSignature(f, kind, true)
    }

    fun mapSignatureWithGeneric(f: FunctionDescriptor, kind: OwnerKind): JvmMethodGenericSignature {
        return mapSignature(f, kind, false)
    }

    fun mapSignatureWithGeneric(f: FunctionDescriptor, kind: OwnerKind, hasSpecialBridge: Boolean): JvmMethodGenericSignature {
        return mapSignature(f, null, kind, false, hasSpecialBridge)
    }

    private fun mapSignature(f: FunctionDescriptor, kind: OwnerKind, skipGenericSignature: Boolean): JvmMethodGenericSignature {
        return mapSignature(f, null, kind, skipGenericSignature, false)
    }

    private fun mapSignature(
        f: FunctionDescriptor,
        resolvedCall: ResolvedCall<*>?,
        kind: OwnerKind,
        skipGenericSignature: Boolean,
        hasSpecialBridge: Boolean
    ): JvmMethodGenericSignature {
        if (f.initialSignatureDescriptor != null && f != f.initialSignatureDescriptor) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if (f.getOverriddenBuiltinReflectingJvmDescriptor() == null || f.containingDeclaration.original is JavaClassDescriptor) {
                return mapSignature(f.initialSignatureDescriptor!!, kind, skipGenericSignature)
            }
        }

        if (f is TypeAliasConstructorDescriptor) {
            return mapSignature(f.underlyingConstructorDescriptor.original, kind, skipGenericSignature)
        }

        if (f is FunctionImportedFromObject) {
            return mapSignature(f.callableFromObject, kind, skipGenericSignature)
        }

        if (f.isSuspendFunctionNotSuspensionView()) {
            return mapSignature(getOrCreateJvmSuspendFunctionView(f), kind, skipGenericSignature)
        }

        if (isDeclarationOfBigArityFunctionInvoke(f) || isDeclarationOfBigArityCreateCoroutineMethod(f)) {
            val builtIns = f.builtIns
            val arrayOfNullableAny = builtIns.getArrayType(Variance.INVARIANT, builtIns.nullableAnyType)
            return mapSignatureWithCustomParameters(
                f, kind, sequenceOf(arrayOfNullableAny), null, null,
                skipGenericSignature = false,
                hasSpecialBridge = false
            )
        }

        val parameterTypes: Sequence<KotlinType>
        val (returnType, returnAsmType) =
            if (languageVersionSettings.supportsFeature(LanguageFeature.PolymorphicSignature) && isPolymorphicSignature(f)) {
                if (resolvedCall == null) {
                    throw UnsupportedOperationException("Cannot determine polymorphic signature without a resolved call: $f")
                }
                parameterTypes = extractPolymorphicParameterTypes(resolvedCall)
                extractPolymorphicReturnType(resolvedCall)
            } else {
                parameterTypes = f.valueParameters.asSequence().map { it.type }
                null to null
            }

        return mapSignatureWithCustomParameters(f, kind, parameterTypes, returnType, returnAsmType, skipGenericSignature, hasSpecialBridge)
    }

    fun mapSignatureWithCustomParameters(
        f: FunctionDescriptor,
        kind: OwnerKind,
        valueParameters: List<ValueParameterDescriptor>,
        skipGenericSignature: Boolean
    ): JvmMethodGenericSignature =
        mapSignatureWithCustomParameters(f, kind, valueParameters.asSequence().map { it.type }, null, null, skipGenericSignature, false)

    private fun mapSignatureWithCustomParameters(
        f: FunctionDescriptor,
        kind: OwnerKind,
        valueParameterTypes: Sequence<KotlinType>,
        customReturnType: KotlinType?,
        customReturnAsmType: Type?,
        skipGenericSignature: Boolean,
        hasSpecialBridge: Boolean
    ): JvmMethodGenericSignature {
        val sw = if (skipGenericSignature || f is AccessorForCallableDescriptor<*>)
            JvmSignatureWriter()
        else
            BothSignatureWriter(BothSignatureWriter.Mode.METHOD)

        if (f is ClassConstructorDescriptor) {
            sw.writeParametersStart()
            writeAdditionalConstructorParameters(f, sw)

            for (type in valueParameterTypes) {
                writeParameter(sw, type, f)
            }

            if (f is AccessorForConstructorDescriptor) {
                writeParameter(sw, JvmMethodParameterKind.CONSTRUCTOR_MARKER, DEFAULT_CONSTRUCTOR_MARKER)
            }

            if (kind == OwnerKind.ERASED_INLINE_CLASS) {
                sw.writeReturnType()
                sw.writeAsmType(mapType(f.containingDeclaration))
                sw.writeReturnTypeEnd()
            } else {
                writeVoidReturn(sw)
            }
        } else {
            val directMember = DescriptorUtils.getDirectMember(f)
            val thisIfNeeded: KotlinType? = when (kind) {
                OwnerKind.DEFAULT_IMPLS -> {
                    val receiverTypeAndTypeParameters = patchTypeParametersForDefaultImplMethod(directMember)
                    writeFormalTypeParameters(receiverTypeAndTypeParameters.typeParameters + directMember.typeParameters, sw)
                    receiverTypeAndTypeParameters.receiverType
                }
                OwnerKind.ERASED_INLINE_CLASS -> {
                    writeFormalTypeParameters(directMember.typeParameters, sw)
                    (directMember.containingDeclaration as ClassDescriptor).defaultType
                }
                else -> {
                    writeFormalTypeParameters(directMember.typeParameters, sw)
                    if (isAccessor(f) && f.dispatchReceiverParameter != null) {
                        (f.containingDeclaration as ClassDescriptor).defaultType
                    } else null
                }
            }

            sw.writeParametersStart()
            if (thisIfNeeded != null) {
                writeParameter(sw, JvmMethodParameterKind.THIS, thisIfNeeded, f)
            }

            for (contextReceiverParameter in f.contextReceiverParameters) {
                writeParameter(sw, JvmMethodParameterKind.CONTEXT_RECEIVER, contextReceiverParameter.type, f)
            }

            val receiverParameter = f.extensionReceiverParameter
            if (receiverParameter != null) {
                writeParameter(sw, JvmMethodParameterKind.RECEIVER, receiverParameter.type, f)
            }

            for (type in valueParameterTypes) {
                val forceBoxing = forceSingleValueParameterBoxing(f)
                writeParameter(sw, if (forceBoxing) TypeUtils.makeNullable(type) else type, f)
            }

            sw.writeReturnType()
            when {
                customReturnAsmType != null -> sw.writeAsmType(customReturnAsmType)
                customReturnType != null -> mapReturnType(f, sw, customReturnType)
                else -> mapReturnType(f, sw)
            }
            sw.writeReturnTypeEnd()
        }

        val signature = sw.makeJvmMethodSignature(mapFunctionName(f, kind))

        if (kind != OwnerKind.DEFAULT_IMPLS && kind != OwnerKind.ERASED_INLINE_CLASS && !hasSpecialBridge) {
            val specialSignatureInfo = with(BuiltinMethodsWithSpecialGenericSignature) { f.getSpecialSignatureInfo() }

            if (specialSignatureInfo != null) {
                val newGenericSignature = specialSignatureInfo.replaceValueParametersIn(signature.genericsSignature)
                return JvmMethodGenericSignature(signature.asmMethod, signature.valueParameters, newGenericSignature)
            }
        }

        return signature
    }

    private fun extractPolymorphicParameterTypes(resolvedCall: ResolvedCall<*>): Sequence<KotlinType> {
        val valueArgumentsByIndex = resolvedCall.valueArgumentsByIndex
        val argument = valueArgumentsByIndex?.singleOrNull() as? VarargValueArgument ?: throw UnsupportedOperationException(
            "Polymorphic signature is only supported for methods with one vararg argument: " + resolvedCall.resultingDescriptor
        )

        return argument.arguments.asSequence().map { arg ->
            val expression = arg.getArgumentExpression() ?: throw UnsupportedOperationException(
                "Polymorphic signature argument must be an expression: " + resolvedCall.resultingDescriptor
            )
            bindingContext.getType(expression)!!
        }
    }

    private fun extractPolymorphicReturnType(resolvedCall: ResolvedCall<*>): Pair<KotlinType?, Type?> {
        // Return type is polymorphic only in case it's Object; see VarHandle.compareAndSet and similar.
        val originalReturnType = resolvedCall.resultingDescriptor.returnType
        if (originalReturnType != null && !KotlinBuiltIns.isAny(originalReturnType)) return null to null

        var expression = resolvedCall.call.callElement as? KtExpression ?: throw UnsupportedOperationException(
            "Polymorphic signature method call must be an expression: " + resolvedCall.resultingDescriptor
        )

        while (true) {
            expression = expression.getQualifiedExpressionForSelector() ?: break
        }
        expression = expression.getOutermostParenthesizerOrThis()

        // See https://docs.oracle.com/javase/11/docs/api/java/lang/invoke/MethodHandle.html
        return when {
            // `invokeExact(...) as X` is generated to `invokevirtual (...)LX;`.
            expression.parent is KtBinaryExpressionWithTypeRHS ->
                bindingContext.getType(expression.parent as KtExpression) to null
            // `invokeExact(...)` without a cast is generated to `invokevirtual (...)V` in a statement context,
            // and to `invokevirtual (...)Ljava/lang/Object;` in an expression context.
            expression.isUsedAsExpression(bindingContext) -> null to OBJECT_TYPE
            else -> null to Type.VOID_TYPE
        }
    }

    fun mapDefaultMethod(functionDescriptor: FunctionDescriptor, kind: OwnerKind): Method {
        val jvmSignature = mapAsmMethod(functionDescriptor, kind)
        val ownerType = mapOwner(functionDescriptor)
        val isConstructor = isConstructor(jvmSignature)
        val descriptor = getDefaultDescriptor(
            jvmSignature,
            if (isStaticMethod(kind, functionDescriptor) || isConstructor) null else ownerType.descriptor,
            functionDescriptor.unwrapFrontendVersion(),
            0
        )

        return Method(if (isConstructor) "<init>" else jvmSignature.name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, descriptor)
    }

    /**
     * @return true iff a given function descriptor should be compiled to a method with boxed return type regardless of whether return type
     * of that descriptor is nullable or not. This happens in two cases:
     * - when a target function is a synthetic box method of erased inline class;
     * - when a function returning a value of a primitive type overrides another function with a non-primitive return type.
     * In that case the generated method's return type should be boxed: otherwise it's not possible to use
     * this class from Java since javac issues errors when loading the class (incompatible return types)
     */
    private fun forceBoxedReturnType(descriptor: FunctionDescriptor): Boolean {
        if (isBoxMethodForInlineClass(descriptor)) return true

        val returnType = descriptor.returnType!!

        // 'invoke' methods for lambdas, function literals, and callable references
        // implicitly override generic 'invoke' from a corresponding base class.
        if ((isFunctionExpression(descriptor) || isFunctionLiteral(descriptor)) && returnType.isInlineClassType()) return true

        return isJvmPrimitive(returnType) &&
                getAllOverriddenDescriptors(descriptor).any { !isJvmPrimitive(it.returnType!!) } ||
                returnType.isInlineClassType() && descriptor is JavaMethodDescriptor
    }

    private fun isJvmPrimitive(kotlinType: KotlinType) =
        KotlinBuiltIns.isPrimitiveType(kotlinType)

    private fun isBoxMethodForInlineClass(descriptor: FunctionDescriptor): Boolean {
        val containingDeclaration = descriptor.containingDeclaration
        return containingDeclaration.isInlineClass() &&
                descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED &&
                descriptor.name == InlineClassDescriptorResolver.BOX_METHOD_NAME
    }

    fun mapFieldSignature(backingFieldType: KotlinType, propertyDescriptor: PropertyDescriptor): String? {
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)

        writeFieldSignature(backingFieldType, propertyDescriptor, sw)

        return sw.makeJavaGenericSignature()
    }

    fun writeFieldSignature(
        backingFieldType: KotlinType,
        variableDescriptor: VariableDescriptor,
        sw: JvmSignatureWriter
    ) {
        if (!variableDescriptor.isVar) {
            mapReturnType(variableDescriptor, sw, backingFieldType)
        } else {
            writeParameterType(sw, backingFieldType, variableDescriptor)
        }
    }

    fun writeFormalTypeParameters(typeParameters: List<TypeParameterDescriptor>, sw: JvmSignatureWriter) {
        if (sw.skipGenericSignature()) return
        for (typeParameter in typeParameters) {
            if (!classBuilderMode.generateBodies && typeParameter.name.isSpecial) {
                // If a type parameter has no name, the code below fails, but it should recover in case of light classes
                continue
            }

            SimpleClassicTypeSystemContext.writeFormalTypeParameter(typeParameter, sw) { type, mode ->
                mapType(type as KotlinType, sw, mode)
            }
        }
    }

    private fun writeParameter(sw: JvmSignatureWriter, type: KotlinType, callableDescriptor: CallableDescriptor?) {
        writeParameter(sw, JvmMethodParameterKind.VALUE, type, callableDescriptor)
    }

    private fun writeParameter(
        sw: JvmSignatureWriter,
        kind: JvmMethodParameterKind,
        type: KotlinType,
        callableDescriptor: CallableDescriptor?
    ) {
        sw.writeParameterType(kind)

        writeParameterType(sw, type, callableDescriptor)

        sw.writeParameterTypeEnd()
    }

    fun writeParameterType(sw: JvmSignatureWriter, type: KotlinType, callableDescriptor: CallableDescriptor?) {
        if (sw.skipGenericSignature()) {
            if (type.isInlineClassType() && callableDescriptor is JavaMethodDescriptor) {
                mapType(type, sw, TypeMappingMode.GENERIC_ARGUMENT)
            } else {
                mapType(type, sw, TypeMappingMode.DEFAULT)
            }
            return
        }

        val typeMappingMode =
            extractTypeMappingModeFromAnnotation(callableDescriptor, type, isForAnnotationParameter = false, mapTypeAliases = false)
                ?: if (callableDescriptor.isMethodWithDeclarationSiteWildcards && type.arguments.isNotEmpty()) {
                    TypeMappingMode.GENERIC_ARGUMENT // Render all wildcards
                } else {
                    typeSystem.getOptimalModeForValueParameter(type)
                }

        mapType(type, sw, typeMappingMode)
    }

    private fun writeAdditionalConstructorParameters(descriptor: ClassConstructorDescriptor, sw: JvmSignatureWriter) {
        val isSynthesized = descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED

        val closure = bindingContext.get(CLOSURE, descriptor.containingDeclaration)

        val captureThis = getDispatchReceiverParameterForConstructorCall(descriptor, closure)
        if (!isSynthesized && captureThis != null) {
            writeParameter(sw, JvmMethodParameterKind.OUTER, captureThis.defaultType, descriptor)
        }

        val captureReceiverType = closure?.capturedReceiverFromOuterContext
        if (captureReceiverType != null) {
            writeParameter(sw, JvmMethodParameterKind.RECEIVER, captureReceiverType, descriptor)
        }

        val containingDeclaration = descriptor.containingDeclaration

        if (!isSynthesized) {
            if (containingDeclaration.kind == ClassKind.ENUM_CLASS || containingDeclaration.kind == ClassKind.ENUM_ENTRY) {
                writeParameter(sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, descriptor.builtIns.stringType, descriptor)
                writeParameter(sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, descriptor.builtIns.intType, descriptor)
            }
        }

        if (closure == null) return

        for (variableDescriptor in closure.captureVariables.keys) {
            val type =
                if (variableDescriptor is VariableDescriptor && variableDescriptor !is PropertyDescriptor) {
                    getSharedVarType(variableDescriptor)
                        ?: if (isDelegatedLocalVariable(variableDescriptor)) {
                            val delegateType =
                                getPropertyDelegateType(variableDescriptor as LocalVariableDescriptor, bindingContext)
                                    ?: error("Local delegated property type should not be null: $variableDescriptor")
                            mapType(delegateType)
                        } else {
                            mapType(variableDescriptor.type)
                        }
                } else if (isLocalFunction(variableDescriptor)) {
                    asmTypeForAnonymousClass(bindingContext, variableDescriptor as FunctionDescriptor)
                } else {
                    null
                }

            if (type != null) {
                closure.setCapturedParameterOffsetInConstructor(variableDescriptor, sw.currentSignatureSize + 1)
                writeParameter(sw, JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE, type)
            }
        }

        // We may generate a slightly wrong signature for a local class / anonymous object in light classes mode but we don't care,
        // because such classes are not accessible from the outside world
        if (classBuilderMode.generateBodies) {
            val superCall = findFirstDelegatingSuperCall(descriptor) ?: return
            writeSuperConstructorCallParameters(sw, descriptor, superCall, captureThis != null)
        }
    }

    private fun writeSuperConstructorCallParameters(
        sw: JvmSignatureWriter,
        descriptor: ClassConstructorDescriptor,
        superCall: ResolvedCall<ConstructorDescriptor>,
        hasOuter: Boolean
    ) {
        val superDescriptor = SamCodegenUtil.resolveSamAdapter(superCall.resultingDescriptor)
        val valueArguments = superCall.valueArguments

        val parameters = mapSignatureSkipGeneric(superDescriptor.original).valueParameters

        val params = parameters.size
        val args = valueArguments.size

        // Mapped parameters should consist of captured values plus all of valueArguments
        assert(params >= args) { "Incorrect number of mapped parameters vs arguments: $params < $args for $descriptor" }

        // Include all captured values, i.e. those parameters for which there are no resolved value arguments
        for (i in 0 until params - args) {
            val parameter = parameters[i]
            val kind = parameter.kind
            if (kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL) continue
            if (hasOuter && kind == JvmMethodParameterKind.OUTER) continue

            writeParameter(sw, JvmMethodParameterKind.SUPER_CALL_PARAM, parameter.asmType)
        }

        if (isAnonymousObject(descriptor.containingDeclaration)) {
            // For anonymous objects, also add all real non-default value arguments passed to the super constructor
            for ((key, valueArgument) in valueArguments) {
                if (valueArgument !is DefaultValueArgument) {
                    val parameter = parameters[params - args + key.index]
                    writeParameter(sw, JvmMethodParameterKind.SUPER_CALL_PARAM, parameter.asmType)
                }
            }
        }
    }

    private fun findFirstDelegatingSuperCall(descriptor: ConstructorDescriptor): ResolvedCall<ConstructorDescriptor>? {
        var current = descriptor
        val constructorOwner = current.containingDeclaration
        val visited = hashSetOf<ConstructorDescriptor>()
        visited.add(current)
        while (true) {
            val next = getDelegationConstructorCall(bindingContext, current) ?: return null
            current = next.resultingDescriptor.original
            if (!visited.add(current)) return null
            if (current.containingDeclaration != constructorOwner) return next
        }
    }

    fun mapSyntheticMethodForPropertyAnnotations(descriptor: PropertyDescriptor): Method {
        val receiver = descriptor.extensionReceiverParameter
        val baseName = if (languageVersionSettings.supportsFeature(LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm)) {
            mapFunctionName(descriptor.getter!!, OwnerKind.IMPLEMENTATION)
        } else descriptor.name.asString()
        val name = JvmAbi.getSyntheticMethodNameForAnnotatedProperty(baseName)
        val desc = if (receiver == null) "()V" else "(${mapType(receiver.type)})V"
        return Method(name, desc)
    }

    fun mapScriptSignature(script: ScriptDescriptor, importedScripts: List<ScriptDescriptor>): JvmMethodSignature {
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.METHOD)

        sw.writeParametersStart()

        if (importedScripts.isNotEmpty()) {
            writeParameter(sw, script.module.builtIns.array.defaultType, null)
        }

        for (valueParameter in script.unsubstitutedPrimaryConstructor.valueParameters) {
            writeParameter(sw, valueParameter.type, null)/* callableDescriptor = */
        }

        writeVoidReturn(sw)

        return sw.makeJvmMethodSignature("<init>")
    }

    fun getSharedVarType(descriptor: DeclarationDescriptor): Type? {
        if (descriptor is SimpleFunctionDescriptor && descriptor.containingDeclaration is FunctionDescriptor) {
            return asmTypeForAnonymousClass(bindingContext, descriptor as FunctionDescriptor)
        }

        if (descriptor is PropertyDescriptor || descriptor is FunctionDescriptor) {
            val receiverParameter = (descriptor as CallableDescriptor).extensionReceiverParameter
                ?: error("Callable should have a receiver parameter: $descriptor")
            return StackValue.sharedTypeForType(mapType(receiverParameter.type))
        }

        if (descriptor is LocalVariableDescriptor && descriptor.isDelegated) {
            return null
        }

        return if (descriptor is VariableDescriptor && isBoxedLocalCapturedInClosure(bindingContext, descriptor)) {
            StackValue.sharedTypeForType(mapType(descriptor.type))
        } else null
    }

    fun classInternalName(classDescriptor: ClassDescriptor): String {
        return typeMappingConfiguration.getPredefinedTypeForClass(classDescriptor)?.internalName
            ?: computeInternalName(classDescriptor, typeMappingConfiguration)
    }

    object InternalNameMapper {
        fun mangleInternalName(name: String, moduleName: String): String {
            return name + "$" + NameUtils.sanitizeAsJavaIdentifier(moduleName)
        }

        fun canBeMangledInternalName(name: String): Boolean {
            return '$' in name
        }

        fun demangleInternalName(name: String): String? {
            val indexOfDollar = name.indexOf('$')
            return if (indexOfDollar >= 0) name.substring(0, indexOfDollar) else null
        }

        fun getModuleNameSuffix(name: String): String? {
            val indexOfDollar = name.indexOf('$')
            return if (indexOfDollar >= 0) name.substring(indexOfDollar + 1) else null
        }

        fun internalNameWithoutModuleSuffix(name: String): String? {
            val demangledName = demangleInternalName(name)
            return if (demangledName != null) "$demangledName$" else null
        }
    }

    companion object {
        /**
         * Use proper LanguageVersionSettings where possible.
         */
        val LANGUAGE_VERSION_SETTINGS_DEFAULT: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

        private fun internalNameForPackageMemberOwner(callableDescriptor: CallableMemberDescriptor, publicFacade: Boolean): String {
            val isAccessor: Boolean
            val descriptor = if (callableDescriptor is AccessorForCallableDescriptor<*>) {
                isAccessor = true
                callableDescriptor.calleeDescriptor
            } else {
                isAccessor = false
                callableDescriptor
            }

            val file = DescriptorToSourceUtils.getContainingFile(descriptor)
            if (file != null) {
                val visibility = descriptor.visibility
                return if (!publicFacade ||
                    isNonConstProperty(descriptor) ||
                    DescriptorVisibilities.isPrivate(visibility) ||
                    isAccessor/*Cause of KT-9603*/) {
                    JvmFileClassUtil.getFileClassInternalName(file)
                } else {
                    JvmFileClassUtil.getFacadeClassInternalName(file)
                }
            }

            val directMember = DescriptorUtils.getDirectMember(descriptor)

            if (directMember is DescriptorWithContainerSource) {
                val facadeFqName = getPackageMemberOwnerInternalName(directMember, publicFacade)
                if (facadeFqName != null) return facadeFqName
            }

            if (directMember is FictitiousArrayConstructor) {
                return "kotlin.Array"
            }

            throw RuntimeException(
                "Could not find package member for " + descriptor +
                        " in package fragment " + descriptor.containingDeclaration
            )
        }

        private fun isNonConstProperty(descriptor: CallableMemberDescriptor): Boolean =
            descriptor is PropertyDescriptor && !descriptor.isConst

        fun getContainingClassesForDeserializedCallable(
            deserializedDescriptor: DescriptorWithContainerSource
        ): ContainingClassesInfo {
            val parentDeclaration = deserializedDescriptor.containingDeclaration

            val containingClassesInfo =
                if (parentDeclaration is PackageFragmentDescriptor) {
                    getPackageMemberContainingClassesInfo(deserializedDescriptor)
                } else {
                    val classId = getContainerClassIdForClassDescriptor(parentDeclaration as ClassDescriptor)
                    ContainingClassesInfo.forClassMember(classId)
                }
            return containingClassesInfo ?: throw IllegalStateException("Couldn't find container for " + deserializedDescriptor.name)
        }

        private fun getContainerClassIdForClassDescriptor(classDescriptor: ClassDescriptor): ClassId {
            val classId = classDescriptor.classId ?: error("Deserialized class should have a ClassId: $classDescriptor")

            val nestedClass: String? = if (isInterface(classDescriptor)) {
                JvmAbi.DEFAULT_IMPLS_SUFFIX
            } else {
                null
            }

            if (nestedClass != null) {
                //TODO test nested trait fun inlining
                val defaultImplsClassName = classId.relativeClassName.shortName().asString() + nestedClass
                return ClassId(classId.packageFqName, Name.identifier(defaultImplsClassName))
            }

            return classId
        }

        private fun getPackageMemberOwnerInternalName(descriptor: DescriptorWithContainerSource, publicFacade: Boolean): String? {
            val containingDeclaration = descriptor.containingDeclaration
            assert(containingDeclaration is PackageFragmentDescriptor) { "Not a top-level member: $descriptor" }

            val containingClasses = getPackageMemberContainingClassesInfo(descriptor) ?: return null

            val ownerClassId = if (publicFacade)
                containingClasses.facadeClassId
            else
                containingClasses.implClassId
            return JvmClassName.byClassId(ownerClassId).internalName
        }

        private val FAKE_CLASS_ID_FOR_BUILTINS = ClassId(FqName("kotlin.jvm.internal"), FqName("Intrinsics.Kotlin"), false)

        private fun getPackageMemberContainingClassesInfo(descriptor: DescriptorWithContainerSource): ContainingClassesInfo? {
            val containingDeclaration = descriptor.containingDeclaration
            if (containingDeclaration is BuiltInsPackageFragment) {
                return ContainingClassesInfo(FAKE_CLASS_ID_FOR_BUILTINS, FAKE_CLASS_ID_FOR_BUILTINS)
            }

            val implClassName = descriptor.getImplClassNameForDeserialized() ?: error("No implClassName for $descriptor")

            val facadeName = when (containingDeclaration) {
                is LazyJavaPackageFragment -> containingDeclaration.getFacadeNameForPartName(implClassName) ?: return null
                is IncrementalMultifileClassPackageFragment -> containingDeclaration.facadeName
                // TODO: for multi-file class part, they can be different
                is PackageFragmentDescriptor -> implClassName
                else -> throw AssertionError(
                    "Unexpected package fragment for $descriptor: $containingDeclaration (${containingDeclaration.javaClass.simpleName})"
                )
            }

            return ContainingClassesInfo.forPackageMember(facadeName, implClassName)
        }

        @JvmStatic
        fun mapUnderlyingTypeOfInlineClassType(kotlinType: KotlinTypeMarker, typeMapper: KotlinTypeMapperBase): Type {
            val underlyingType = with(typeMapper.typeSystem) {
                kotlinType.getUnsubstitutedUnderlyingType()
            } ?: throw IllegalStateException("There should be underlying type for inline class type: $kotlinType")
            return typeMapper.mapTypeCommon(underlyingType, TypeMappingMode.DEFAULT)
        }

        internal fun generateErrorMessageForErrorType(type: KotlinType, descriptor: DeclarationDescriptor): String {
            val declarationElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
                ?: return "Error type encountered: $type (${type.javaClass.simpleName})."

            val containingDeclaration = descriptor.containingDeclaration
            val parentDeclarationElement =
                if (containingDeclaration != null) DescriptorToSourceUtils.descriptorToDeclaration(containingDeclaration) else null

            return "Error type encountered: %s (%s). Descriptor: %s. For declaration %s:%s in %s:%s".format(
                type,
                type.javaClass.simpleName,
                descriptor,
                declarationElement,
                declarationElement.text,
                parentDeclarationElement,
                if (parentDeclarationElement != null) parentDeclarationElement.text else "null"
            )
        }

        private fun getJvmShortName(klass: ClassDescriptor): String {
            return JavaToKotlinClassMap.mapKotlinToJava(getFqName(klass))?.shortClassName?.asString()
                ?: SpecialNames.safeIdentifier(klass.name).identifier
        }

        private fun hasNothingInNonContravariantPosition(kotlinType: KotlinType): Boolean =
            SimpleClassicTypeSystemContext.hasNothingInNonContravariantPosition(kotlinType)

        fun TypeSystemContext.hasNothingInNonContravariantPosition(type: KotlinTypeMarker): Boolean {
            val typeConstructor = type.typeConstructor()

            for (i in 0 until type.argumentsCount()) {
                val projection = type.getArgument(i)
                if (projection.isStarProjection()) continue

                val argument = projection.getType()

                if (argument.isNullableNothing() ||
                    argument.isNothing() && typeConstructor.getParameter(i).getVariance() != TypeVariance.IN
                ) return true
            }

            return false
        }

        fun getVarianceForWildcard(parameter: TypeParameterDescriptor, projection: TypeProjection, mode: TypeMappingMode): Variance =
            SimpleClassicTypeSystemContext.getVarianceForWildcard(parameter, projection, mode)

        private fun TypeSystemCommonBackendContext.getVarianceForWildcard(
            parameter: TypeParameterMarker, projection: TypeArgumentMarker, mode: TypeMappingMode
        ): Variance {
            val projectionKind = projection.getVariance().convertVariance()
            val parameterVariance = parameter.getVariance().convertVariance()

            if (parameterVariance == Variance.INVARIANT) {
                return projectionKind
            }

            if (mode.skipDeclarationSiteWildcards) {
                return Variance.INVARIANT
            }

            if (projectionKind == Variance.INVARIANT || projectionKind == parameterVariance) {
                if (mode.skipDeclarationSiteWildcardsIfPossible && !projection.isStarProjection()) {
                    if (parameterVariance == Variance.OUT_VARIANCE && isMostPreciseCovariantArgument(projection.getType())) {
                        return Variance.INVARIANT
                    }

                    if (parameterVariance == Variance.IN_VARIANCE && isMostPreciseContravariantArgument(projection.getType(), parameter)) {
                        return Variance.INVARIANT
                    }
                }
                return parameterVariance
            }

            // In<out X> = In<*>
            // Out<in X> = Out<*>
            return Variance.OUT_VARIANCE
        }

        fun TypeSystemCommonBackendContext.writeGenericArguments(
            signatureVisitor: JvmSignatureWriter,
            arguments: List<TypeArgumentMarker>,
            parameters: List<TypeParameterMarker>,
            mode: TypeMappingMode,
            mapType: (KotlinTypeMarker, JvmSignatureWriter, TypeMappingMode) -> Type
        ) {
            for ((parameter, argument) in parameters.zip(arguments)) {
                if (argument.isStarProjection() ||
                    // In<Nothing, Foo> == In<*, Foo> -> In<?, Foo>
                    argument.getType().isNothing() && parameter.getVariance() == TypeVariance.IN
                ) {
                    signatureVisitor.writeUnboundedWildcard()
                } else {
                    val argumentMode = mode.updateArgumentModeFromAnnotations(argument.getType(), this)
                    val projectionKind = getVarianceForWildcard(parameter, argument, argumentMode)

                    signatureVisitor.writeTypeArgument(projectionKind)

                    mapType(
                        argument.getType(), signatureVisitor,
                        argumentMode.toGenericArgumentMode(
                            getEffectiveVariance(parameter.getVariance().convertVariance(), argument.getVariance().convertVariance())
                        )
                    )

                    signatureVisitor.writeTypeArgumentEnd()
                }
            }
        }

        //NB: similar platform agnostic code in DescriptorUtils.unwrapFakeOverride
        private fun findSuperDeclaration(descriptor: FunctionDescriptor, isSuperCall: Boolean, jvmDefaultMode: JvmDefaultMode): FunctionDescriptor {
            var current = descriptor
            while (current.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                val classCallable = current.overriddenDescriptors.firstOrNull { !isInterface(it.containingDeclaration) }
                if (classCallable != null) {
                    //prefer class callable cause of else branch
                    current = classCallable
                    continue
                }
                if (isSuperCall && !current.isCompiledToJvmDefault(jvmDefaultMode) && !isInterface(current.containingDeclaration)) {
                    //Don't unwrap fake overrides from class to interface cause substituted override would be implicitly generated
                    return current
                }

                current = current.overriddenDescriptors.firstOrNull()
                    ?: error("Fake override should have at least one overridden descriptor: $current")
            }
            return current
        }

        @JvmStatic
        fun isAccessor(descriptor: CallableMemberDescriptor?): Boolean {
            return descriptor is AccessorForCallableDescriptor<*> || descriptor is AccessorForCompanionObjectInstanceFieldDescriptor
        }

        @JvmStatic
        fun isStaticAccessor(descriptor: CallableMemberDescriptor?): Boolean {
            return if (descriptor is AccessorForConstructorDescriptor) false else isAccessor(descriptor)
        }

        internal fun findAnyDeclaration(function: FunctionDescriptor): FunctionDescriptor {
            return if (function.kind == CallableMemberDescriptor.Kind.DECLARATION) {
                function
            } else findBaseDeclaration(function)
        }

        private fun findBaseDeclaration(function: FunctionDescriptor): FunctionDescriptor {
            return if (function.overriddenDescriptors.isEmpty()) {
                function
            } else {
                // TODO: prefer class to interface
                findBaseDeclaration(function.overriddenDescriptors.iterator().next())
            }
        }

        private fun getKindForDefaultImplCall(baseMethodDescriptor: FunctionDescriptor): OwnerKind {
            val containingDeclaration = baseMethodDescriptor.containingDeclaration
            return when {
                containingDeclaration is PackageFragmentDescriptor -> OwnerKind.PACKAGE
                isInterface(containingDeclaration) -> OwnerKind.DEFAULT_IMPLS
                containingDeclaration.isInlineClass() -> OwnerKind.ERASED_INLINE_CLASS
                else -> OwnerKind.IMPLEMENTATION
            }
        }

        @JvmStatic
        fun mapDefaultFieldName(propertyDescriptor: PropertyDescriptor, isDelegated: Boolean): String {
            val name: String =
                if (propertyDescriptor is AccessorForPropertyDescriptor) {
                    propertyDescriptor.calleeDescriptor.name.asString()
                } else {
                    propertyDescriptor.name.asString()
                }
            return if (isDelegated) name + JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX else name
        }

        @JvmField
        val BOX_JVM_METHOD_NAME = InlineClassDescriptorResolver.BOX_METHOD_NAME.toString() + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS

        @JvmField
        val UNBOX_JVM_METHOD_NAME = InlineClassDescriptorResolver.UNBOX_METHOD_NAME.toString() + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS

        private fun getPartSimpleNameForMangling(callableDescriptor: CallableMemberDescriptor): String? {
            var descriptor = callableDescriptor
            val containingFile = DescriptorToSourceUtils.getContainingFile(descriptor)
            if (containingFile != null) {
                val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(containingFile)
                return if (fileClassInfo.withJvmMultifileClass) {
                    fileClassInfo.fileClassFqName.shortName().asString()
                } else null
            }

            descriptor = DescriptorUtils.getDirectMember(descriptor)
            assert(descriptor is DeserializedCallableMemberDescriptor) {
                "Descriptor without sources should be instance of DeserializedCallableMemberDescriptor, but: $descriptor"
            }
            val containingClassesInfo = getContainingClassesForDeserializedCallable(descriptor as DeserializedCallableMemberDescriptor)
            val facadeShortName = containingClassesInfo.facadeClassId.shortClassName.asString()
            val implShortName = containingClassesInfo.implClassId.shortClassName.asString()
            return if (facadeShortName != implShortName) implShortName else null
        }

        private fun getDefaultDescriptor(
            method: Method,
            dispatchReceiverDescriptor: String?,
            callableDescriptor: CallableDescriptor,
            extraArgsShift: Int
        ): String {
            val descriptor = method.descriptor
            val maskArgumentsCount = (callableDescriptor.valueParameters.size - extraArgsShift + Integer.SIZE - 1) / Integer.SIZE
            val defaultConstructorMarkerType = if (isConstructor(method) || isInlineClassConstructor(callableDescriptor))
                DEFAULT_CONSTRUCTOR_MARKER
            else
                OBJECT_TYPE
            val additionalArgs = StringUtil.repeat(Type.INT_TYPE.descriptor, maskArgumentsCount) + defaultConstructorMarkerType.descriptor
            val result = descriptor.replace(")", "$additionalArgs)")
            return if (dispatchReceiverDescriptor != null && !isConstructor(method)) {
                result.replace("(", "($dispatchReceiverDescriptor")
            } else result
        }

        private fun isConstructor(method: Method): Boolean {
            return "<init>" == method.name
        }

        private fun isInlineClassConstructor(callableDescriptor: CallableDescriptor): Boolean {
            return callableDescriptor is ClassConstructorDescriptor && callableDescriptor.containingDeclaration.isInlineClass()
        }

        private fun writeVoidReturn(sw: JvmSignatureWriter) {
            sw.writeReturnType()
            sw.writeAsmType(Type.VOID_TYPE)
            sw.writeReturnTypeEnd()
        }

        private fun writeParameter(sw: JvmSignatureWriter, kind: JvmMethodParameterKind, type: Type) {
            sw.writeParameterType(kind)
            sw.writeAsmType(type)
            sw.writeParameterTypeEnd()
        }

        @JvmStatic
        fun TypeSystemCommonBackendContext.writeFormalTypeParameter(
            typeParameter: TypeParameterMarker,
            sw: JvmSignatureWriter,
            mapType: (KotlinTypeMarker, TypeMappingMode) -> Type
        ) {
            sw.writeFormalTypeParameter(typeParameter.getName().asString())

            sw.writeClassBound()

            for (i in 0 until typeParameter.upperBoundCount()) {
                val type = typeParameter.getUpperBound(i)
                if (type.typeConstructor().getTypeParameterClassifier() == null && !type.isInterfaceOrAnnotationClass()) {
                    mapType(type, TypeMappingMode.GENERIC_ARGUMENT)
                    break
                }
            }

            // "extends Object" is optional according to ClassFileFormat-Java5.pdf
            // but javac complaints to signature:
            // <P:>Ljava/lang/Object;
            // TODO: avoid writing java/lang/Object if interface list is not empty

            sw.writeClassBoundEnd()

            for (i in 0 until typeParameter.upperBoundCount()) {
                val type = typeParameter.getUpperBound(i)
                if (type.typeConstructor().getTypeParameterClassifier() != null || type.isInterfaceOrAnnotationClass()) {
                    sw.writeInterfaceBound()
                    mapType(type, TypeMappingMode.GENERIC_ARGUMENT)
                    sw.writeInterfaceBoundEnd()
                }
            }
        }
    }
}
