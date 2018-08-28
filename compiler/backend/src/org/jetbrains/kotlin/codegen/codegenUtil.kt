/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.PackageContext
import org.jetbrains.kotlin.codegen.coroutines.unwrapInitialDescriptorForSuspendFunction
import org.jetbrains.kotlin.codegen.inline.ReificationArgument
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isSubclass
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor.CoroutinesCompatibilityMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

fun generateIsCheck(
    v: InstructionAdapter,
    kotlinType: KotlinType,
    asmType: Type,
    isReleaseCoroutines: Boolean
) {
    if (TypeUtils.isNullableType(kotlinType)) {
        val nope = Label()
        val end = Label()

        with(v) {
            dup()

            ifnull(nope)

            TypeIntrinsics.instanceOf(this, kotlinType, asmType, isReleaseCoroutines)

            goTo(end)

            mark(nope)
            pop()
            iconst(1)

            mark(end)
        }
    } else {
        TypeIntrinsics.instanceOf(v, kotlinType, asmType, isReleaseCoroutines)
    }
}

fun generateAsCast(
    v: InstructionAdapter,
    kotlinType: KotlinType,
    asmType: Type,
    isSafe: Boolean,
    isReleaseCoroutines: Boolean
) {
    if (!isSafe) {
        if (!TypeUtils.isNullableType(kotlinType)) {
            generateNullCheckForNonSafeAs(v, kotlinType)
        }
    } else {
        with(v) {
            dup()
            TypeIntrinsics.instanceOf(v, kotlinType, asmType, isReleaseCoroutines)
            val ok = Label()
            ifne(ok)
            pop()
            aconst(null)
            mark(ok)
        }
    }

    TypeIntrinsics.checkcast(v, kotlinType, asmType, isSafe)
}

private fun generateNullCheckForNonSafeAs(
    v: InstructionAdapter,
    type: KotlinType
) {
    with(v) {
        dup()
        val nonnull = Label()
        ifnonnull(nonnull)
        AsmUtil.genThrow(
            v,
            "kotlin/TypeCastException",
            "null cannot be cast to non-null type " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)
        )
        mark(nonnull)
    }
}

fun SpecialSignatureInfo.replaceValueParametersIn(sourceSignature: String?): String? =
    valueParametersSignature?.let { sourceSignature?.replace("^\\(.*\\)".toRegex(), "($it)") }

fun populateCompanionBackingFieldNamesToOuterContextIfNeeded(
    companion: KtObjectDeclaration,
    outerContext: FieldOwnerContext<*>,
    state: GenerationState
) {
    val descriptor = state.bindingContext.get(BindingContext.CLASS, companion)

    if (descriptor == null || ErrorUtils.isError(descriptor)) {
        return
    }

    if (!JvmAbi.isClassCompanionObjectWithBackingFieldsInOuter(descriptor)) {
        return
    }
    val properties = companion.declarations.filterIsInstance<KtProperty>()

    properties.forEach {
        val variableDescriptor = state.bindingContext.get(BindingContext.VARIABLE, it)
        if (variableDescriptor is PropertyDescriptor) {
            outerContext.getFieldName(variableDescriptor, it.hasDelegate())
        }
    }

}

// TODO: inline and remove then ScriptCodegen is converted to Kotlin
fun mapSupertypesNames(
    typeMapper: KotlinTypeMapper,
    supertypes: List<ClassDescriptor>,
    signatureVisitor: JvmSignatureWriter?
): Array<String> =
    supertypes.map { typeMapper.mapSupertype(it.defaultType, signatureVisitor).internalName }.toTypedArray()


// Top level subclasses of a sealed class should be generated before that sealed class,
// so that we'd generate the necessary accessor for its constructor afterwards
fun sortTopLevelClassesAndPrepareContextForSealedClasses(
    classOrObjects: List<KtClassOrObject>,
    packagePartContext: PackageContext,
    state: GenerationState
): List<KtClassOrObject> {
    fun prepareContextIfNeeded(descriptor: ClassDescriptor?) {
        if (DescriptorUtils.isSealedClass(descriptor)) {
            // save context for sealed class
            packagePartContext.intoClass(descriptor!!, OwnerKind.IMPLEMENTATION, state)
        }
    }

    // optimization
    when (classOrObjects.size) {
        0 -> return emptyList()
        1 -> {
            prepareContextIfNeeded(state.bindingContext.get(BindingContext.CLASS, classOrObjects.first()))
            return classOrObjects
        }
    }

    val result = ArrayList<KtClassOrObject>(classOrObjects.size)
    val descriptorToPsi = LinkedHashMap<ClassDescriptor, KtClassOrObject>()
    for (classOrObject in classOrObjects) {
        val descriptor = state.bindingContext.get(BindingContext.CLASS, classOrObject)
        if (descriptor == null) {
            result.add(classOrObject)
        } else {
            prepareContextIfNeeded(descriptor)
            descriptorToPsi[descriptor] = classOrObject
        }
    }

    // topologicalOrder(listOf(1, 2, 3)) { emptyList() } = listOf(3, 2, 1). Because of this used keys.reversed().
    val sortedDescriptors = DFS.topologicalOrder(descriptorToPsi.keys.reversed()) {
        it.typeConstructor.supertypes.map { it.constructor.declarationDescriptor as? ClassDescriptor }.filter { it in descriptorToPsi.keys }
    }
    sortedDescriptors.mapTo(result) { descriptorToPsi[it]!! }
    return result
}

fun CallableMemberDescriptor.isDefinitelyNotDefaultImplsMethod() =
    this is JavaCallableMemberDescriptor || this.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)


fun ClassBuilder.generateMethod(
    debugString: String,
    access: Int,
    method: Method,
    element: PsiElement?,
    origin: JvmDeclarationOrigin,
    state: GenerationState,
    generate: InstructionAdapter.() -> Unit
) {
    val mv = this.newMethod(origin, access, method.name, method.descriptor, null, null)

    if (state.classBuilderMode.generateBodies) {
        val iv = InstructionAdapter(mv)
        iv.visitCode()
        iv.generate()
        iv.areturn(method.returnType)
        FunctionCodegen.endVisit(mv, debugString, element)
    }
}

fun CallableDescriptor.isJvmStaticInObjectOrClassOrInterface(): Boolean =
    isJvmStaticIn {
        DescriptorUtils.isNonCompanionObject(it) ||
                // This is necessary because for generation of @JvmStatic methods from companion of class A
                // we create a synthesized descriptor containing in class A
                DescriptorUtils.isClassOrEnumClass(it) || DescriptorUtils.isInterface(it)
    }

fun CallableDescriptor.isJvmStaticInCompanionObject(): Boolean =
    isJvmStaticIn { DescriptorUtils.isCompanionObject(it) }

private fun CallableDescriptor.isJvmStaticIn(predicate: (DeclarationDescriptor) -> Boolean): Boolean =
    when (this) {
        is PropertyAccessorDescriptor -> {
            val propertyDescriptor = correspondingProperty
            predicate(propertyDescriptor.containingDeclaration) &&
                    (hasJvmStaticAnnotation() || propertyDescriptor.hasJvmStaticAnnotation())
        }
        else -> predicate(containingDeclaration) && hasJvmStaticAnnotation()
    }

fun Collection<VariableDescriptor>.filterOutDescriptorsWithSpecialNames() = filterNot { it.name.isSpecial }

class JvmKotlinType(val type: Type, val kotlinType: KotlinType? = null)

fun KotlinType.asmType(typeMapper: KotlinTypeMapper) = typeMapper.mapType(this)

fun KtExpression?.asmType(typeMapper: KotlinTypeMapper, bindingContext: BindingContext): Type =
    this.kotlinType(bindingContext)?.asmType(typeMapper) ?: Type.VOID_TYPE

fun KtExpression?.kotlinType(bindingContext: BindingContext) = this?.let(bindingContext::getType)

fun Collection<Type>.withVariableIndices(): List<Pair<Int, Type>> = mutableListOf<Pair<Int, Type>>().apply {
    var index = 0
    for (type in this@withVariableIndices) {
        add(index to type)
        index += type.size
    }
}

fun FunctionDescriptor.isGenericToArray(): Boolean {
    if (name.asString() != "toArray") return false
    if (valueParameters.size != 1 || typeParameters.size != 1) return false

    val returnType = returnType ?: throw AssertionError(toString())
    val paramType = valueParameters[0].type

    if (!KotlinBuiltIns.isArray(returnType) || !KotlinBuiltIns.isArray(paramType)) return false

    val elementType = typeParameters[0].defaultType
    return KotlinTypeChecker.DEFAULT.equalTypes(elementType, builtIns.getArrayElementType(returnType)) &&
            KotlinTypeChecker.DEFAULT.equalTypes(elementType, builtIns.getArrayElementType(paramType))
}

fun FunctionDescriptor.isNonGenericToArray(): Boolean {
    if (name.asString() != "toArray") return false
    if (!valueParameters.isEmpty() || !typeParameters.isEmpty()) return false

    val returnType = returnType
    return returnType != null && KotlinBuiltIns.isArray(returnType)
}

fun MemberDescriptor.isToArrayFromCollection(): Boolean {
    if (this !is FunctionDescriptor) return false

    val containingClassDescriptor = containingDeclaration as? ClassDescriptor ?: return false
    if (containingClassDescriptor.source == SourceElement.NO_SOURCE) return false

    val collectionClass = builtIns.collection
    if (!isSubclass(containingClassDescriptor, collectionClass)) return false

    return isGenericToArray() || isNonGenericToArray()
}

fun FqName.topLevelClassInternalName() = JvmClassName.byClassId(ClassId(parent(), shortName())).internalName
fun FqName.topLevelClassAsmType(): Type = Type.getObjectType(topLevelClassInternalName())

fun initializeVariablesForDestructuredLambdaParameters(codegen: ExpressionCodegen, valueParameters: List<ValueParameterDescriptor>) {
    val savedIsShouldMarkLineNumbers = codegen.isShouldMarkLineNumbers
    // Do not write line numbers until destructuring happens
    // (otherwise destructuring variables will be uninitialized in the beginning of lambda)
    codegen.isShouldMarkLineNumbers = false

    for (parameterDescriptor in valueParameters) {
        if (parameterDescriptor !is ValueParameterDescriptorImpl.WithDestructuringDeclaration) continue

        for (entry in parameterDescriptor.destructuringVariables.filterOutDescriptorsWithSpecialNames()) {
            codegen.myFrameMap.enter(entry, codegen.typeMapper.mapType(entry.type))
        }

        val destructuringDeclaration =
            (DescriptorToSourceUtils.descriptorToDeclaration(parameterDescriptor) as? KtParameter)?.destructuringDeclaration
                ?: error("Destructuring declaration for descriptor $parameterDescriptor not found")

        codegen.initializeDestructuringDeclarationVariables(
            destructuringDeclaration,
            TransientReceiver(parameterDescriptor.type),
            codegen.findLocalOrCapturedValue(parameterDescriptor) ?: error("Local var not found for parameter $parameterDescriptor")
        )
    }

    codegen.isShouldMarkLineNumbers = savedIsShouldMarkLineNumbers
}

fun <D : CallableDescriptor> D.unwrapFrontendVersion() = unwrapInitialDescriptorForSuspendFunction()

inline fun FrameMap.useTmpVar(type: Type, block: (index: Int) -> Unit) {
    val index = enterTemp(type)
    block(index)
    leaveTemp(type)
}

fun InstructionAdapter.generateNewInstanceDupAndPlaceBeforeStackTop(
    frameMap: FrameMap,
    topStackType: Type,
    newInstanceInternalName: String
) {
    frameMap.useTmpVar(topStackType) { index ->
        store(index, topStackType)
        anew(Type.getObjectType(newInstanceInternalName))
        dup()
        load(index, topStackType)
    }
}

fun extractReificationArgument(type: KotlinType): Pair<TypeParameterDescriptor, ReificationArgument>? {
    var type = type
    var arrayDepth = 0
    val isNullable = type.isMarkedNullable
    while (KotlinBuiltIns.isArray(type)) {
        arrayDepth++
        type = type.arguments[0].type
    }

    val parameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type) ?: return null

    return Pair(parameterDescriptor, ReificationArgument(parameterDescriptor.name.asString(), isNullable, arrayDepth))
}

fun unwrapInitialSignatureDescriptor(function: FunctionDescriptor): FunctionDescriptor =
    function.initialSignatureDescriptor ?: function

fun ExpressionCodegen.generateCallReceiver(call: ResolvedCall<out CallableDescriptor>): StackValue =
    generateReceiverValue(call.extensionReceiver ?: call.dispatchReceiver!!, false)

fun ExpressionCodegen.generateCallSingleArgument(call: ResolvedCall<out CallableDescriptor>): StackValue =
    gen(call.getFirstArgumentExpression()!!)

fun ClassDescriptor.isPossiblyUninitializedSingleton() =
    DescriptorUtils.isEnumEntry(this) ||
            DescriptorUtils.isCompanionObject(this) && JvmCodegenUtil.isJvmInterface(this.containingDeclaration)

val CodegenContext<*>.parentContextsWithSelf
    get() = generateSequence(this) { it.parentContext }

val CodegenContext<*>.parentContexts
    get() = parentContext?.parentContextsWithSelf ?: emptySequence()

val CodegenContext<*>.contextStackText
    get() = parentContextsWithSelf.joinToString(separator = "\n") { it.toString() }

inline fun FrameMap.evaluateOnce(
    value: StackValue,
    asType: Type,
    v: InstructionAdapter,
    body: (StackValue) -> Unit
) {
    val valueOrTmp: StackValue =
        if (value.canHaveSideEffects())
            StackValue.local(enterTemp(asType), asType).apply { store(value, v) }
        else
            value

    body(valueOrTmp)

    if (valueOrTmp != value) {
        leaveTemp(asType)
    }
}

// Handy debugging routine. Print all instructions from methodNode.
fun MethodNode.textifyMethodNode(): String {
    val text = Textifier()
    val tmv = TraceMethodVisitor(text)
    this.instructions.asSequence().forEach { it.accept(tmv) }
    localVariables.forEach { text.visitLocalVariable(it.name, it.desc, it.signature, it.start.label, it.end.label, it.index) }
    val sw = StringWriter()
    text.print(PrintWriter(sw))
    return "$sw"
}

fun KotlinType.isInlineClassTypeWithPrimitiveEquality(): Boolean {
    if (!isInlineClassType()) return false

    // Always treat unsigned types as inline classes with primitive equality
    if (UnsignedTypes.isUnsignedType(this)) return true

    // TODO support other inline classes that can be compared as underlying primitives
    return false
}

fun CallableDescriptor.needsExperimentalCoroutinesWrapper() =
    (this as? DeserializedMemberDescriptor)?.coroutinesExperimentalCompatibilityMode == CoroutinesCompatibilityMode.NEEDS_WRAPPER
