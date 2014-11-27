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

import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.descriptors.serialization.*
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotatedCallableKind
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.ProtoContainer
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.types.ErrorUtils

import java.util.*

import org.jetbrains.jet.lang.resolve.kotlin.DescriptorLoadersStorage.MemberSignature
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.javaClassIdToKotlinClassId

public class AnnotationDescriptorLoader(
        private val module: ModuleDescriptor,
        storage: DescriptorLoadersStorage,
        kotlinClassFinder: KotlinClassFinder,
        errorReporter: ErrorReporter
) : BaseDescriptorLoader(kotlinClassFinder, errorReporter, storage), AnnotationLoader {

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<AnnotationDescriptor> {
        val classId = nameResolver.getClassId(classProto.getFqName())
        val kotlinClass = findKotlinClassById(classId)
        if (kotlinClass == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: KotlinClassFinder had found the
            // class earlier, but it can't now
            errorReporter.reportLoadingError("Kotlin class for loading class annotations is not found: ${classId.asSingleFqName()}", null)
            return listOf()
        }

        val result = ArrayList<AnnotationDescriptor>(1)

        kotlinClass.loadClassAnnotations(object : KotlinJvmBinaryClass.AnnotationVisitor {
            override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                return resolveAnnotation(classId, result, module)
            }

            override fun visitEnd() {
            }
        })

        return result
    }

    override fun loadCallableAnnotations(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind
    ): List<AnnotationDescriptor> {
        val signature = getCallableSignature(proto, nameResolver, kind) ?: return listOf()
        return findClassAndLoadMemberAnnotations(container, proto, nameResolver, kind, signature)
    }

    private fun findClassAndLoadMemberAnnotations(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind,
            signature: MemberSignature
    ): List<AnnotationDescriptor> {
        val kotlinClass = findClassWithAnnotationsAndInitializers(container, proto, nameResolver, kind)
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading member annotations is not found: $container", null)
            return listOf()
        }

        return storage.getStorageForClass(kotlinClass).memberAnnotations[signature] ?: listOf()
    }

    override fun loadValueParameterAnnotations(
            container: ProtoContainer,
            callable: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind,
            proto: ProtoBuf.Callable.ValueParameter
    ): List<AnnotationDescriptor> {
        val methodSignature = getCallableSignature(callable, nameResolver, kind)
        if (methodSignature != null) {
            if (proto.hasExtension(JavaProtoBuf.index)) {
                val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, proto.getExtension(JavaProtoBuf.index))
                return findClassAndLoadMemberAnnotations(container, callable, nameResolver, kind, paramSignature)
            }
        }

        return listOf()
    }

    class object {

        public fun resolveAnnotation(
                classId: ClassId,
                result: MutableList<AnnotationDescriptor>,
                moduleDescriptor: ModuleDescriptor
        ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
            if (JvmAnnotationNames.isSpecialAnnotation(classId, true)) return null

            val annotationClass = resolveClass(classId, moduleDescriptor)

            return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                private val arguments = HashMap<ValueParameterDescriptor, CompileTimeConstant<*>>()

                override fun visit(name: Name?, value: Any?) {
                    if (name != null) {
                        setArgumentValueByName(name, createConstant(name, value))
                    }
                }

                override fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name) {
                    setArgumentValueByName(name, enumEntryValue(enumClassId, enumEntryName))
                }

                override fun visitArray(name: Name): AnnotationArrayArgumentVisitor? {
                    return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                        private val elements = ArrayList<CompileTimeConstant<*>>()

                        override fun visit(value: Any?) {
                            elements.add(createConstant(name, value))
                        }

                        override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                            elements.add(enumEntryValue(enumClassId, enumEntryName))
                        }

                        override fun visitEnd() {
                            val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                            if (parameter != null) {
                                elements.trimToSize()
                                arguments[parameter] = ArrayValue(elements, parameter.getType(), true, false)
                            }
                        }
                    }
                }

                private fun enumEntryValue(enumClassId: ClassId, name: Name): CompileTimeConstant<*> {
                    val enumClass = resolveClass(enumClassId, moduleDescriptor)
                    if (enumClass.getKind() == ClassKind.ENUM_CLASS) {
                        val classifier = enumClass.getUnsubstitutedInnerClassesScope().getClassifier(name)
                        if (classifier is ClassDescriptor) {
                            return EnumValue(classifier, false)
                        }
                    }
                    return ErrorValue.create("Unresolved enum entry: $enumClassId.$name")
                }

                override fun visitEnd() {
                    result.add(AnnotationDescriptorImpl(annotationClass.getDefaultType(), arguments))
                }

                private fun createConstant(name: Name?, value: Any?): CompileTimeConstant<*> {
                    return createCompileTimeConstant(value, true, false, false, null)
                           ?: ErrorValue.create("Unsupported annotation argument: $name")
                }

                private fun setArgumentValueByName(name: Name, argumentValue: CompileTimeConstant<*>) {
                    val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                    if (parameter != null) {
                        arguments[parameter] = argumentValue
                    }
                }
            }
        }

        private fun resolveClass(javaClassId: ClassId, moduleDescriptor: ModuleDescriptor): ClassDescriptor {
            val classId = javaClassIdToKotlinClassId(javaClassId)
            return moduleDescriptor.findClassAcrossModuleDependencies(classId)
                   ?: ErrorUtils.createErrorClass(classId.asSingleFqName().asString())
        }
    }
}
