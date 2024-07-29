/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.*
import org.jetbrains.kotlin.codegen.replaceValueParametersIn
import org.jetbrains.kotlin.codegen.sanitizeNameIfNeeded
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
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
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.convertVariance
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.zipWithNulls
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class KotlinTypeMapper @JvmOverloads constructor(
    private val moduleName: String,
    val languageVersionSettings: LanguageVersionSettings,
    private val useOldInlineClassesManglingScheme: Boolean,
    private val typePreprocessor: ((KotlinType) -> KotlinType?)? = null,
    private val namePreprocessor: ((ClassDescriptor) -> String?)? = null
) : KotlinTypeMapperBase() {
    override val typeSystem: TypeSystemCommonBackendContext
        get() = SimpleClassicTypeSystemContext

    private val typeMappingConfiguration = object : TypeMappingConfiguration<Type> {
        override fun commonSupertype(types: Collection<KotlinType>): KotlinType {
            return CommonSupertypes.commonSupertype(types)
        }

        override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): Type? {
            return null
        }

        override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? {
            return getPredefinedTypeForClass(classDescriptor)?.internalName
        }

        override fun getPredefinedFullInternalNameForClass(classDescriptor: ClassDescriptor): String? {
            return namePreprocessor?.invoke(classDescriptor)
        }

        override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
        }

        override fun preprocessType(kotlinType: KotlinType): KotlinType? {
            return typePreprocessor?.invoke(kotlinType)
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

    override fun mapClass(classifier: ClassifierDescriptor): Type {
        return mapType(classifier.defaultType, null, TypeMappingMode.CLASS_DECLARATION)
    }

    override fun mapTypeCommon(type: KotlinTypeMarker, mode: TypeMappingMode): Type {
        return mapType(type as KotlinType, null, mode)
    }

    @JvmOverloads
    fun mapType(
        type: KotlinType,
        signatureVisitor: JvmSignatureWriter? = null,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT
    ): Type {
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

            signatureVisitor.writeOuterClassBegin(asmType, mapType(outerType.classDescriptor.defaultType).internalName)
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
                classDescriptor.functionTypeKind == FunctionTypeKind.KFunction ||
                classDescriptor.functionTypeKind == FunctionTypeKind.KSuspendFunction
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

    fun mapFunctionName(descriptor: FunctionDescriptor): String {
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

                val propertyName = property.name.asString()

                val accessorName = if (descriptor is PropertyGetterDescriptor)
                    JvmAbi.getterName(propertyName)
                else
                    JvmAbi.setterName(propertyName)

                mangleMemberNameIfRequired(accessorName, descriptor)
            }
            isFunctionLiteral(descriptor) -> {
                OperatorNameConventions.INVOKE.asString()
            }
            isLocalFunction(descriptor) || isFunctionExpression(descriptor) ->
                OperatorNameConventions.INVOKE.asString()
            else ->
                mangleMemberNameIfRequired(descriptor.name.asString(), descriptor)
        }
    }

    private fun ClassDescriptor.hasJavaLangRecordSupertype() =
        typeConstructor.supertypes.any { KotlinBuiltIns.isConstructedFromGivenClass(it, JAVA_LANG_RECORD_FQ_NAME) }

    private val shouldMangleByReturnType =
        languageVersionSettings.supportsFeature(LanguageFeature.MangleClassMembersReturningInlineClasses)

    private fun mangleMemberNameIfRequired(
        name: String,
        descriptor: CallableMemberDescriptor,
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

        if (descriptor is ConstructorDescriptor) return name
        var newName = name

        val suffix = getManglingSuffixBasedOnKotlinSignature(
            descriptor,
            shouldMangleByReturnType,
            useOldInlineClassesManglingScheme
        )
        if (suffix != null) {
            newName += suffix
        }

        newName = sanitizeNameIfNeeded(newName, languageVersionSettings)

        if (isTopLevelDeclaration(descriptor)) {
            if (DescriptorVisibilities.isPrivate(descriptor.visibility) && "<clinit>" != newName) {
                val partName = getPartSimpleNameForMangling(descriptor)
                if (partName != null) return "$newName$$partName"
            }
            return newName
        }

        return if (descriptor.visibility === DescriptorVisibilities.INTERNAL && !descriptor.isPublishedApi()) {
            InternalNameMapper.mangleInternalName(newName, getModuleName(descriptor))
        } else newName
    }

    private fun getModuleName(descriptor: CallableMemberDescriptor): String {
        return getJvmModuleNameForDeserializedDescriptor(descriptor) ?: moduleName
    }

    fun mapAsmMethod(descriptor: FunctionDescriptor): Method {
        return mapSignature(descriptor, true).asmMethod
    }

    private fun mapSignature(f: FunctionDescriptor, skipGenericSignature: Boolean): JvmMethodGenericSignature {
        if (f.initialSignatureDescriptor != null && f != f.initialSignatureDescriptor) {
            // Overrides of special builtin in Kotlin classes always have special signature
            if (f.getOverriddenBuiltinReflectingJvmDescriptor() == null || f.containingDeclaration.original is JavaClassDescriptor) {
                return mapSignature(f.initialSignatureDescriptor!!, skipGenericSignature)
            }
        }

        if (f is TypeAliasConstructorDescriptor) {
            return mapSignature(f.underlyingConstructorDescriptor.original, skipGenericSignature)
        }

        if (f is FunctionImportedFromObject) {
            return mapSignature(f.callableFromObject, skipGenericSignature)
        }

        val valueParameterTypes =
            if (isDeclarationOfBigArityFunctionInvoke(f) || isDeclarationOfBigArityCreateCoroutineMethod(f)) {
                listOf(f.builtIns.getArrayType(Variance.INVARIANT, f.builtIns.nullableAnyType))
            } else {
                f.valueParameters.map { it.type }
            }

        val sw = JvmSignatureWriter()

        if (f is ClassConstructorDescriptor) {
            sw.writeParametersStart()
            writeAdditionalConstructorParameters(f, sw)

            for (type in valueParameterTypes) {
                writeParameter(sw, type, f)
            }

            writeVoidReturn(sw)
        } else {
            writeFormalTypeParameters(getDirectMember(f).typeParameters, sw)

            sw.writeParametersStart()

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
            mapReturnType(f, sw)
            sw.writeReturnTypeEnd()
        }

        val signature = sw.makeJvmMethodSignature(mapFunctionName(f))

        val specialSignatureInfo = with(BuiltinMethodsWithSpecialGenericSignature) { f.getSpecialSignatureInfo() }

        if (specialSignatureInfo != null) {
            val newGenericSignature = specialSignatureInfo.replaceValueParametersIn(signature.genericsSignature)
            return JvmMethodGenericSignature(signature.asmMethod, signature.valueParameters, newGenericSignature)
        }

        return signature
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

    private fun writeFormalTypeParameters(typeParameters: List<TypeParameterDescriptor>, sw: JvmSignatureWriter) {
        if (sw.skipGenericSignature()) return
        for (typeParameter in typeParameters) {
            if (typeParameter.name.isSpecial) {
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

        val captureThis = getDispatchReceiverParameterForConstructorCall(descriptor)
        if (!isSynthesized && captureThis != null) {
            writeParameter(sw, JvmMethodParameterKind.OUTER, captureThis.defaultType, descriptor)
        }

        val containingDeclaration = descriptor.containingDeclaration

        if (!isSynthesized) {
            if (containingDeclaration.kind == ClassKind.ENUM_CLASS || containingDeclaration.kind == ClassKind.ENUM_ENTRY) {
                writeParameter(sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, descriptor.builtIns.stringType, descriptor)
                writeParameter(sw, JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL, descriptor.builtIns.intType, descriptor)
            }
        }
    }

    object InternalNameMapper {
        fun mangleInternalName(name: String, moduleName: String): String {
            return name + "$" + NameUtils.sanitizeAsJavaIdentifier(moduleName)
        }
    }

    companion object {
        private fun getContainingClassesForDeserializedCallable(
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

        private val FAKE_CLASS_ID_FOR_BUILTINS = ClassId(FqName("kotlin.jvm.internal"), FqName("Intrinsics.Kotlin"), isLocal = false)

        private fun getPackageMemberContainingClassesInfo(descriptor: DescriptorWithContainerSource): ContainingClassesInfo? {
            val containingDeclaration = descriptor.containingDeclaration
            if (containingDeclaration is BuiltInsPackageFragment) {
                return ContainingClassesInfo(FAKE_CLASS_ID_FOR_BUILTINS, FAKE_CLASS_ID_FOR_BUILTINS)
            }

            val implClassName = descriptor.getImplClassNameForDeserialized() ?: error("No implClassName for $descriptor")

            val facadeName = when (containingDeclaration) {
                is LazyJavaPackageFragment -> containingDeclaration.getFacadeNameForPartName(implClassName) ?: return null
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
                kotlinType.typeConstructor().getUnsubstitutedUnderlyingType()
            } ?: throw IllegalStateException("There should be underlying type for inline class type: $kotlinType")
            return typeMapper.mapTypeCommon(underlyingType, TypeMappingMode.DEFAULT)
        }

        private fun getJvmShortName(klass: ClassDescriptor): String {
            return JavaToKotlinClassMap.mapKotlinToJava(getFqName(klass))?.shortClassName?.asString()
                ?: SpecialNames.safeIdentifier(klass.name).identifier
        }

        private fun hasNothingInNonContravariantPosition(kotlinType: KotlinType): Boolean =
            SimpleClassicTypeSystemContext.hasNothingInNonContravariantPosition(kotlinType)

        fun TypeSystemContext.hasNothingInNonContravariantPosition(type: KotlinTypeMarker): Boolean {
            if (type.isError()) {
                // We cannot access type arguments for an unresolved type
                return false
            }

            val typeConstructor = type.typeConstructor()

            for (i in 0 until type.argumentsCount()) {
                val projection = type.getArgument(i)
                val argument = projection.getType() ?: continue

                if (argument.isNullableNothing() ||
                    argument.isNothing() && typeConstructor.getParameter(i).getVariance() != TypeVariance.IN
                ) return true
            }

            return false
        }

        // Used from KSP.
        @Suppress("unused")
        fun getVarianceForWildcard(parameter: TypeParameterDescriptor, projection: TypeProjection, mode: TypeMappingMode): Variance =
            SimpleClassicTypeSystemContext.getVarianceForWildcard(parameter, projection, mode)

        fun TypeSystemCommonBackendContext.getVarianceForWildcard(
            parameter: TypeParameterMarker?, projection: TypeArgumentMarker, mode: TypeMappingMode
        ): Variance {
            val projectionKind = projection.getVariance().convertVariance()
            val parameterVariance = parameter?.getVariance()?.convertVariance() ?: Variance.INVARIANT

            if (parameterVariance == Variance.INVARIANT) {
                return projectionKind
            }

            if (mode.skipDeclarationSiteWildcards) {
                return Variance.INVARIANT
            }

            if (projectionKind == Variance.INVARIANT || projectionKind == parameterVariance) {
                val type = projection.getType()
                if (mode.skipDeclarationSiteWildcardsIfPossible && type != null) {
                    if (parameterVariance == Variance.OUT_VARIANCE && isMostPreciseCovariantArgument(type)) {
                        return Variance.INVARIANT
                    }

                    if (parameterVariance == Variance.IN_VARIANCE && isMostPreciseContravariantArgument(type)) {
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
            processGenericArguments(
                arguments,
                parameters,
                mode,
                processUnboundedWildcard = {
                    signatureVisitor.writeUnboundedWildcard()
                },
                processTypeArgument = { _, type, projectionKind, _, newMode ->
                    signatureVisitor.writeTypeArgument(projectionKind)
                    mapType(type, signatureVisitor, newMode)
                    signatureVisitor.writeTypeArgumentEnd()
                }
            )
        }

        fun TypeSystemCommonBackendContext.processGenericArguments(
            arguments: List<TypeArgumentMarker>,
            parameters: List<TypeParameterMarker>,
            mode: TypeMappingMode,
            processUnboundedWildcard: () -> Unit,
            processTypeArgument: (index: Int, type: KotlinTypeMarker, projectionKind: Variance, parameterVariance: Variance, mode: TypeMappingMode) -> Unit,
        ) {
            for ((index, pair) in parameters.zipWithNulls(arguments).withIndex()) {
                val (parameter, argument) = pair
                if (argument == null) break
                val type = argument.getType()
                if (type == null ||
                    // In<Nothing, Foo> == In<*, Foo> -> In<?, Foo>
                    type.isNothing() && parameter?.getVariance() == TypeVariance.IN
                ) {
                    processUnboundedWildcard()
                } else {
                    val argumentMode = mode.updateArgumentModeFromAnnotations(type, this)
                    val projectionKind = getVarianceForWildcard(parameter, argument, argumentMode)
                    val parameterVariance = parameter?.getVariance()?.convertVariance() ?: Variance.INVARIANT
                    val newMode = argumentMode.toGenericArgumentMode(
                        getEffectiveVariance(parameterVariance, argument.getVariance().convertVariance())
                    )
                    processTypeArgument(index, type, projectionKind, parameterVariance, newMode)
                }
            }
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

            descriptor = getDirectMember(descriptor)
            assert(descriptor is DeserializedCallableMemberDescriptor) {
                "Descriptor without sources should be instance of DeserializedCallableMemberDescriptor, but: $descriptor"
            }
            val containingClassesInfo = getContainingClassesForDeserializedCallable(descriptor as DeserializedCallableMemberDescriptor)
            val facadeShortName = containingClassesInfo.facadeClassId.shortClassName.asString()
            val implShortName = containingClassesInfo.implClassId.shortClassName.asString()
            return if (facadeShortName != implShortName) implShortName else null
        }

        private fun writeVoidReturn(sw: JvmSignatureWriter) {
            sw.writeReturnType()
            sw.writeAsmType(Type.VOID_TYPE)
            sw.writeReturnTypeEnd()
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
