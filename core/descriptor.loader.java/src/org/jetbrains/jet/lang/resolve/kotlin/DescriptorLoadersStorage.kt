/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin

import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.storage.StorageManager

import java.util.*
import kotlin.platform.platformStatic

public class DescriptorLoadersStorage(storageManager: StorageManager, private val module: ModuleDescriptor) {
    private val storage = storageManager.createMemoizedFunction<KotlinJvmBinaryClass, Storage>{
        kotlinClass ->
        loadAnnotationsAndInitializers(kotlinClass)
    }

    public fun getStorageForClass(kotlinClass: KotlinJvmBinaryClass): Storage = storage(kotlinClass)

    private fun loadAnnotationsAndInitializers(kotlinClass: KotlinJvmBinaryClass): Storage {
        val memberAnnotations = HashMap<MemberSignature, MutableList<AnnotationDescriptor>>()
        val propertyConstants = HashMap<MemberSignature, CompileTimeConstant<*>>()

        kotlinClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString() + desc))
            }

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                val signature = MemberSignature.fromFieldNameAndDesc(name, desc)

                if (initializer != null) {
                    val normalizedValue: Any
                    if (desc in "ZBCS") {
                        val intValue = initializer as Int
                        if ("Z" == desc) {
                            normalizedValue = intValue != 0
                        }
                        else if ("B" == desc) {
                            normalizedValue = (intValue.toByte())
                        }
                        else if ("C" == desc) {
                            normalizedValue = (intValue.toChar())
                        }
                        else if ("S" == desc) {
                            normalizedValue = (intValue.toShort())
                        }
                        else {
                            throw AssertionError(desc)
                        }
                    }
                    else {
                        normalizedValue = initializer
                    }

                    propertyConstants[signature] = createCompileTimeConstant(
                            normalizedValue, canBeUsedInAnnotation = true, isPureIntConstant = true,
                            usesVariableAsConstant = true, expectedType = null
                    )
                }
                return MemberAnnotationVisitor(signature)
            }

            inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature), KotlinJvmBinaryClass.MethodAnnotationVisitor {

                override fun visitParameterAnnotation(index: Int, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                    var result = memberAnnotations[paramSignature]
                    if (result == null) {
                        result = ArrayList<AnnotationDescriptor>()
                        memberAnnotations[paramSignature] = result
                    }
                    return AnnotationDescriptorLoader.resolveAnnotation(classId, result, module)
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<AnnotationDescriptor>()

                override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return AnnotationDescriptorLoader.resolveAnnotation(classId, result, module)
                }

                override fun visitEnd() {
                    if (result.isNotEmpty()) {
                        memberAnnotations[signature] = result
                    }
                }
            }
        })

        return Storage(memberAnnotations, propertyConstants)
    }

    // The purpose of this class is to hold a unique signature of either a method or a field, so that annotations on a member can be put
    // into a map indexed by these signatures
    public data class MemberSignature private(private val signature: String) {

        override fun toString(): String {
            return signature
        }

        class object {
            platformStatic public fun fromMethodNameAndDesc(nameAndDesc: String): MemberSignature {
                return MemberSignature(nameAndDesc)
            }

            platformStatic public fun fromFieldNameAndDesc(name: Name, desc: String): MemberSignature {
                return MemberSignature(name.asString() + "#" + desc)
            }

            platformStatic public fun fromMethodSignatureAndParameterIndex(signature: MemberSignature, index: Int): MemberSignature {
                return MemberSignature(signature.signature + "@" + index)
            }
        }
    }

    class Storage(
            public val memberAnnotations: Map<MemberSignature, List<AnnotationDescriptor>>,
            public val propertyConstants: Map<MemberSignature, CompileTimeConstant<*>>
    ) {
        class object {
            public val EMPTY: Storage = Storage(mapOf(), mapOf())
        }
    }
}
