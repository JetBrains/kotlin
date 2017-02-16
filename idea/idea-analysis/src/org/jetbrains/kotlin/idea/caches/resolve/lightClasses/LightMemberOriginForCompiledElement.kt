/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.MapPsiToAsmDesc
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledTextIndexer
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

interface LightMemberOriginForCompiledElement : LightMemberOrigin {
    override val originKind: JvmDeclarationOriginKind
        get() = JvmDeclarationOriginKind.OTHER
}


data class LightMemberOriginForCompiledField(val psiField: PsiField, val file: KtClsFile) : LightMemberOriginForCompiledElement {
    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForCompiledField(psiField.copy() as PsiField, file)
    }

    override val originalElement: KtDeclaration? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val desc = MapPsiToAsmDesc.typeDesc(psiField.type)
        val signature = MemberSignature.fromFieldNameAndDesc(psiField.name!!, desc)
        findDeclarationInCompiledFile(file, psiField, signature)
    }
}

data class LightMemberOriginForCompiledMethod(val psiMethod: PsiMethod, val file: KtClsFile) : LightMemberOriginForCompiledElement {
    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForCompiledMethod(psiMethod.copy() as PsiMethod, file)
    }

    override val originalElement: KtDeclaration? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val desc = MapPsiToAsmDesc.methodDesc(psiMethod)
        val signature = MemberSignature.fromMethodNameAndDesc(psiMethod.name, desc)
        findDeclarationInCompiledFile(file, psiMethod, signature)
    }
}

private fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration? {
    val relativeClassName = member.relativeClassName()
    val key = ClassNameAndSignature(relativeClassName, signature)

    val memberName = member.name

    if (memberName != null && !file.isContentsLoaded && file.hasDeclarationWithKey(ByJvmSignatureIndexer, key)) {
        val container: KtDeclarationContainer? = if (relativeClassName.isEmpty())
            file
        else {
            val topClassOrObject = file.declarations.singleOrNull() as? KtClassOrObject
            relativeClassName.fold<Name, KtClassOrObject?>(topClassOrObject) { classOrObject, name ->
                classOrObject?.declarations?.singleOrNull { it.name == name.asString() } as? KtClassOrObject
            }
        }

        val declaration = container?.declarations?.singleOrNull {
            it.name == memberName
        }

        if (declaration != null) {
            return declaration
        }
    }

    return file.getDeclaration(ByJvmSignatureIndexer, key)
}

// this is convenient data structure for this purpose and is not supposed to be used outside this file
// every member is represented by its jvm signature and relative class name (which is easy to obtain from descriptors or cls psi)

// relative class name is a path containing inner/nested class names from top level class to the class containing this member (excluding top level class name)
// Examples: for top level function or function in a top level class relativeClassName is empty
// For: class TopLevel { class A { class B { fun f() } } }
// relativeClassName for function 'f' will be [A, B]
private data class ClassNameAndSignature(val relativeClassName: List<Name>, val memberSignature: MemberSignature)

private fun PsiMember.relativeClassName(): List<Name> {
    return generateSequence(this.containingClass) { it.containingClass }.toList().dropLast(1).reversed().map { Name.identifier(it.name!!) }
}

private fun ClassDescriptor.relativeClassName(): List<Name> {
    return classId!!.relativeClassName.pathSegments().drop(1).orEmpty()
}

private fun ClassDescriptor.desc(): String = "L" + JvmClassName.byClassId(classId!!).internalName + ";"

private object ByJvmSignatureIndexer : DecompiledTextIndexer<ClassNameAndSignature> {
    override fun indexDescriptor(descriptor: DeclarationDescriptor): Collection<ClassNameAndSignature> {
        val signatures = arrayListOf<ClassNameAndSignature>()

        fun save(id: List<Name>, signature: MemberSignature) {
            signatures.add(ClassNameAndSignature(id, signature))
        }

        if (descriptor is ClassDescriptor) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (descriptor.kind) {
                ClassKind.ENUM_ENTRY -> {
                    val enumClass = descriptor.containingDeclaration as ClassDescriptor
                    val signature = MemberSignature.fromFieldNameAndDesc(descriptor.name.asString(), enumClass.desc())
                    save(enumClass.relativeClassName(), signature)
                }
                ClassKind.OBJECT -> {
                    val instanceFieldSignature = MemberSignature.fromFieldNameAndDesc(JvmAbi.INSTANCE_FIELD, descriptor.desc())
                    save(descriptor.relativeClassName(), instanceFieldSignature)
                    if (descriptor.isCompanionObject) {
                        val signature = MemberSignature.fromFieldNameAndDesc(descriptor.name.asString(), descriptor.desc())
                        save((descriptor.containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty(), signature)
                    }
                }
            }
        }

        if (descriptor is DeserializedSimpleFunctionDescriptor) {
            JvmProtoBufUtil.getJvmMethodSignature(descriptor.proto, descriptor.nameResolver, descriptor.typeTable)?.let {
                val signature = MemberSignature.fromMethodNameAndDesc(it)
                save((descriptor.containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty(), signature)
            }
        }
        if (descriptor is DeserializedPropertyDescriptor) {
            val proto = descriptor.proto
            val className = (descriptor.containingDeclaration as? ClassDescriptor)?.relativeClassName().orEmpty()
            if (proto.hasExtension(JvmProtoBuf.propertySignature)) {
                val signature = proto.getExtension(JvmProtoBuf.propertySignature)
                val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, descriptor.nameResolver, descriptor.typeTable)
                if (fieldSignature != null) {
                    save(className, MemberSignature.fromFieldNameAndDesc(fieldSignature.name, fieldSignature.desc))
                }
                if (signature.hasGetter()) {
                    save(className, MemberSignature.fromMethod(descriptor.nameResolver, signature.getter))
                }
                if (signature.hasSetter()) {
                    save(className, MemberSignature.fromMethod(descriptor.nameResolver, signature.setter))
                }
            }
        }
        return signatures
    }
}

// expose with different type
val BySignatureIndexer: DecompiledTextIndexer<*> = ByJvmSignatureIndexer
