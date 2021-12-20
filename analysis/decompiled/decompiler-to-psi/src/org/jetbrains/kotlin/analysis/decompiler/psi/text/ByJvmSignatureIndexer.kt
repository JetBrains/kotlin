/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import com.intellij.psi.PsiMember
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.util.cls.ClsFormatException
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.text.CharacterIterator
import java.text.StringCharacterIterator

private object ByJvmSignatureIndexer : DecompiledTextIndexer<ClassNameAndSignature> {
    override fun indexDescriptor(descriptor: DeclarationDescriptor): Collection<ClassNameAndSignature> {
        val signatures = arrayListOf<ClassNameAndSignature>()
        fun save(id: List<Name>, signature: MemberSignature) {
            signatures.add(ClassNameAndSignature(id, signature))
        }

        fun ClassDescriptor.apply() {
            when (kind) {
                ClassKind.ENUM_ENTRY -> {
                    val enumClass = containingDeclaration as ClassDescriptor
                    val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), enumClass.desc())
                    save(enumClass.relativeClassName(), signature)
                }
                ClassKind.OBJECT -> {
                    val instanceFieldSignature = MemberSignature.fromFieldNameAndDesc(JvmAbi.INSTANCE_FIELD, desc())
                    save(relativeClassName(), instanceFieldSignature)
                    if (isCompanionObject) {
                        val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc())
                        save((containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty(), signature)
                    }
                }
                else -> {
                }
            }
        }

        fun DeserializedClassConstructorDescriptor.apply() {
            JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable)?.let {
                val id = (containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty()
                val signature = MemberSignature.fromJvmMemberSignature(it)
                save(id, signature)
            }
        }

        fun DeserializedSimpleFunctionDescriptor.apply() {
            JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable)?.let {
                val id = (containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty()

                val signature = MemberSignature.fromJvmMemberSignature(it)
                save(id, signature)

                if (findJvmOverloadsAnnotation() == null) return

                val extensionShift = if (isExtension) 1 else 0

                val omittedList = mutableListOf<Int>()
                valueParameters.asReversed().forEach { parameter ->
                    if (parameter.hasDefaultValue()) {
                        omittedList.add(parameter.index + extensionShift)
                        val newDescriptor = excludeParametersFromDescriptor(it.desc, omittedList)
                        if (newDescriptor != null) {
                            val overloadedSignature = MemberSignature.fromMethodNameAndDesc(it.name, newDescriptor)
                            save(id, overloadedSignature)
                        }
                    }
                }
            }
        }

        fun DeserializedPropertyDescriptor.apply() {
            val className = (containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty()
            val signature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
            if (signature != null) {
                val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable)
                if (fieldSignature != null) {
                    save(className, MemberSignature.fromJvmMemberSignature(fieldSignature))
                }
                if (signature.hasGetter()) {
                    save(className, MemberSignature.fromMethod(nameResolver, signature.getter))
                }
                if (signature.hasSetter()) {
                    save(className, MemberSignature.fromMethod(nameResolver, signature.setter))
                }
            }
        }

        when (descriptor) {
            is ClassDescriptor -> descriptor.apply()
            is DeserializedClassConstructorDescriptor -> descriptor.apply()
            is DeserializedSimpleFunctionDescriptor -> descriptor.apply()
            is DeserializedPropertyDescriptor -> descriptor.apply()
        }

        return signatures
    }
}

private fun excludeParametersFromDescriptor(descriptor: String, omittedParameters: List<Int>): String? {

    fun tryParseParametersAndReturnType(): Pair<List<String>, String>? {
        val iterator = StringCharacterIterator(descriptor)

        fun parseTypeString(): String? {
            val begin = iterator.index
            try {
                SignatureParsing.parseTypeString(iterator) { it }
            } catch (e: ClsFormatException) {
                return null
            }
            val end = iterator.index
            return descriptor.substring(begin, end)
        }

        if (iterator.current() != '(') return null
        iterator.next()

        if (iterator.current() == ')') {
            iterator.next()
            val returnType = parseTypeString() ?: return null
            return emptyList<String>() to returnType
        }

        val parameterTypes = mutableListOf<String>()
        while (iterator.current() != ')' && iterator.current() != CharacterIterator.DONE) {
            parameterTypes += parseTypeString() ?: return null
        }

        if (iterator.current() != ')') return null
        iterator.next()

        val returnType = parseTypeString() ?: return null
        return parameterTypes to returnType
    }

    val (parameterTypes, returnType) = tryParseParametersAndReturnType() ?: return null

    val parametersList = parameterTypes
        .filterIndexed { index, _ -> index !in omittedParameters }
        .joinToString("")

    return "($parametersList)$returnType"
}

private fun ClassDescriptor.desc(): String = "L" + JvmClassName.byClassId(classId!!).internalName + ";"

fun PsiMember.relativeClassName(): List<Name> {
    return generateSequence(this.containingClass) { it.containingClass }.toList().dropLast(1).reversed().map { Name.identifier(it.name!!) }
}

private fun ClassDescriptor.relativeClassName(): List<Name> {
    return classId!!.relativeClassName.pathSegments().drop(1)
}

// every member is represented by its jvm signature and relative class name (which is easy to obtain from descriptors or cls psi)

// relative class name is a path containing inner/nested class names from top level class to the class containing this member (excluding top level class name)
// Examples: for top level function or function in a top level class relativeClassName is empty
// For: class TopLevel { class A { class B { fun f() } } }
// relativeClassName for function 'f' will be [A, B]
data class ClassNameAndSignature(val relativeClassName: List<Name>, val memberSignature: MemberSignature)

// expose with different type
val BySignatureIndexer: DecompiledTextIndexer<*> = ByJvmSignatureIndexer
