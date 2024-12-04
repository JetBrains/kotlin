/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface
import org.jetbrains.kotlin.codegen.inline.ReificationArgument
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.SpecialSignatureInfo
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isSubclass
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.LabelNode

fun generateIsCheck(
    v: InstructionAdapter,
    kotlinType: KotlinType,
    asmType: Type
) {
    if (TypeUtils.isNullableType(kotlinType)) {
        val nope = Label()
        val end = Label()

        with(v) {
            dup()

            ifnull(nope)

            TypeIntrinsics.instanceOf(this, kotlinType, asmType)

            goTo(end)

            mark(nope)
            pop()
            iconst(1)

            mark(end)
        }
    } else {
        TypeIntrinsics.instanceOf(v, kotlinType, asmType)
    }
}

fun generateAsCast(
    v: InstructionAdapter,
    kotlinType: KotlinType,
    asmType: Type,
    isSafe: Boolean,
    unifiedNullChecks: Boolean,
) {
    if (!isSafe) {
        if (!TypeUtils.isNullableType(kotlinType)) {
            generateNullCheckForNonSafeAs(v, kotlinType, unifiedNullChecks)
        }
    } else {
        with(v) {
            dup()
            TypeIntrinsics.instanceOf(v, kotlinType, asmType)
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
    type: KotlinType,
    unifiedNullChecks: Boolean,
) {
    with(v) {
        dup()
        val nonnull = Label()
        ifnonnull(nonnull)
        val exceptionClass = if (unifiedNullChecks) "java/lang/NullPointerException" else "kotlin/TypeCastException"
        AsmUtil.genThrow(
            v,
            exceptionClass,
            "null cannot be cast to non-null type " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)
        )
        mark(nonnull)
    }
}

fun SpecialSignatureInfo.replaceValueParametersIn(sourceSignature: String?): String? =
    valueParametersSignature?.let { sourceSignature?.replace("^\\(.*\\)".toRegex(), "($it)") }

fun CallableDescriptor.isJvmStaticInObjectOrClassOrInterface(): Boolean =
    isJvmStaticIn {
        DescriptorUtils.isNonCompanionObject(it) ||
                // This is necessary because for generation of @JvmStatic methods from companion of class A
                // we create a synthesized descriptor containing in class A
                DescriptorUtils.isClassOrEnumClass(it) || isJvmInterface(it)
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

class JvmKotlinType(val type: Type, val kotlinType: KotlinType? = null)

fun KtExpression?.kotlinType(bindingContext: BindingContext) = this?.let(bindingContext::getType)

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
    if (valueParameters.isNotEmpty() || typeParameters.isNotEmpty()) return false

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

val CallableDescriptor.arity: Int
    get() = valueParameters.size +
            (if (extensionReceiverParameter != null) 1 else 0) +
            (if (dispatchReceiverParameter != null) 1 else 0)

fun FqName.topLevelClassInternalName() = JvmClassName.internalNameByClassId(ClassId(parent(), shortName()))
fun FqName.topLevelClassAsmType(): Type = Type.getObjectType(topLevelClassInternalName())

inline fun FrameMap.useTmpVar(type: Type, block: (index: Int) -> Unit) {
    val index = enterTemp(type)
    block(index)
    leaveTemp(type)
}

fun TypeSystemCommonBackendContext.extractReificationArgument(initialType: KotlinTypeMarker): Pair<TypeParameterMarker, ReificationArgument>? {
    var type = initialType
    var arrayDepth = 0
    val isNullable = type.isMarkedNullable()
    while (type.isArrayOrNullableArray()) {
        arrayDepth++
        // TODO: warn that nullability info on argument will be lost?
        val argument = type.getArgument(0)
        type = argument.getType() ?: return null
    }

    val typeParameter = type.typeConstructor().getTypeParameterClassifier() ?: return null
    if (!typeParameter.isReified()) return null
    return Pair(typeParameter, ReificationArgument(typeParameter.getName().asString(), isNullable, arrayDepth))
}

fun TypeSystemCommonBackendContext.extractUsedReifiedParameters(type: KotlinTypeMarker): ReifiedTypeParametersUsages =
    ReifiedTypeParametersUsages().apply {
        fun KotlinTypeMarker.visit() {
            val typeParameter = typeConstructor().getTypeParameterClassifier()
            if (typeParameter == null) {
                for (argument in getArguments()) {
                    argument.getType()?.visit()
                }
            } else if (typeParameter.isReified()) {
                addUsedReifiedParameter(typeParameter.getName().asString())
            }
        }

        type.visit()
    }

internal fun LabelNode.linkWithLabel(): LabelNode {
    // Remember labelNode in label and vise versa.
    // Before ASM 8 there was JB patch in MethodNode that makes such linking in constructor of LabelNode.
    //
    // protected LabelNode getLabelNode(final Label label) {
    //    if (!(label.info instanceof LabelNode)) {
    //      //label.info = new LabelNode(label); //[JB: needed for Coverage agent]
    //      label.info = new LabelNode(); //ASM 8
    //    }
    //    return (LabelNode) label.info;
    //  }
    if (label.info == null) {
        label.info = this
    }
    return this
}

fun linkedLabel(): Label = LabelNode().linkWithLabel().label

// Strings in constant pool contain at most 2^16-1 = 65535 bytes.
const val STRING_UTF8_ENCODING_BYTE_LIMIT: Int = 65535

//Each CHAR could be encoded maximum in 3 bytes
fun String.isDefinitelyFitEncodingLimit() = length <= STRING_UTF8_ENCODING_BYTE_LIMIT / 3

fun splitStringConstant(value: String): List<String> {
    return if (value.isDefinitelyFitEncodingLimit()) {
        listOf(value)
    } else {
        val result = arrayListOf<String>()

        // Split strings into parts, each of which satisfies JVM class file constant pool constraints.
        // Note that even if we split surrogate pairs between parts, they will be joined on concatenation.
        var accumulatedSize = 0
        var charOffsetInString = 0
        var lastStringBeginning = 0
        val length = value.length
        while (charOffsetInString < length) {
            val charCode = value[charOffsetInString].code
            val encodedCharSize = when {
                charCode in 1..127 -> 1
                charCode <= 2047 -> 2
                else -> 3
            }
            if (accumulatedSize + encodedCharSize > STRING_UTF8_ENCODING_BYTE_LIMIT) {
                result.add(value.substring(lastStringBeginning, charOffsetInString))
                lastStringBeginning = charOffsetInString
                accumulatedSize = 0
            }
            accumulatedSize += encodedCharSize
            ++charOffsetInString
        }
        result.add(value.substring(lastStringBeginning, charOffsetInString))

        result
    }
}

fun AnnotationVisitor.visitWithSplitting(name: String?, value: String) {
    val av = visitArray(name)
    for (part in splitStringConstant(value)) {
        av.visit(null, part)
    }
    av.visitEnd()
}

fun String.encodedUTF8Size(): Int {
    var result = 0
    for (char in this) {
        val charCode = char.code
        when {
            charCode in 1..127 -> result++
            charCode <= 2047 -> result += 2
            else -> result += 3
        }
    }
    return result
}
