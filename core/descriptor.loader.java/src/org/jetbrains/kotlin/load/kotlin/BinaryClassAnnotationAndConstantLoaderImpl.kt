/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import java.util.*

class BinaryClassAnnotationAndConstantLoaderImpl(
        private val module: ModuleDescriptor,
        storageManager: StorageManager,
        kotlinClassFinder: KotlinClassFinder,
        errorReporter: ErrorReporter
) : AbstractBinaryClassAnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>, AnnotationWithTarget>(
        storageManager, kotlinClassFinder, errorReporter
) {
    private val annotationDeserializer = AnnotationDeserializer(module)
    private val factory = ConstantValueFactory(module.builtIns)

    override fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationDescriptor =
            annotationDeserializer.deserializeAnnotation(proto, nameResolver)

    override fun loadConstant(desc: String, initializer: Any): ConstantValue<*>? {
        val normalizedValue: Any = if (desc in "ZBCS") {
            val intValue = initializer as Int
            when (desc) {
                "Z" -> intValue != 0
                "B" -> intValue.toByte()
                "C" -> intValue.toChar()
                "S" -> intValue.toShort()
                else -> throw AssertionError(desc)
            }
        }
        else {
            initializer
        }

        return factory.createConstantValue(normalizedValue)
    }

    override fun loadPropertyAnnotations(
            propertyAnnotations: List<AnnotationDescriptor>,
            fieldAnnotations: List<AnnotationDescriptor>,
            fieldUseSiteTarget: AnnotationUseSiteTarget
    ): List<AnnotationWithTarget> {
        return propertyAnnotations.map { AnnotationWithTarget(it, null) } +
               fieldAnnotations.map { AnnotationWithTarget(it, fieldUseSiteTarget) }
    }

    override fun transformAnnotations(annotations: List<AnnotationDescriptor>): List<AnnotationWithTarget> {
        return annotations.map { AnnotationWithTarget(it, null) }
    }

    override fun loadAnnotation(
            annotationClassId: ClassId,
            source: SourceElement,
            result: MutableList<AnnotationDescriptor>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val annotationClass = resolveClass(annotationClassId)

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val arguments = HashMap<ValueParameterDescriptor, ConstantValue<*>>()

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
                    private val elements = ArrayList<ConstantValue<*>>()

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
                            arguments[parameter] = factory.createArrayValue(elements, parameter.type)
                        }
                    }
                }
            }

            override fun visitAnnotation(name: Name, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                val list = ArrayList<AnnotationDescriptor>()
                val visitor = loadAnnotation(classId, SourceElement.NO_SOURCE, list)!!
                return object: KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        setArgumentValueByName(name, AnnotationValue(list.single()))
                    }
                }
            }

            // NOTE: see analogous code in AnnotationDeserializer
            private fun enumEntryValue(enumClassId: ClassId, name: Name): ConstantValue<*> {
                val enumClass = resolveClass(enumClassId)
                if (enumClass.kind == ClassKind.ENUM_CLASS) {
                    val classifier = enumClass.unsubstitutedInnerClassesScope.getContributedClassifier(name, NoLookupLocation.FROM_JAVA_LOADER)
                    if (classifier is ClassDescriptor) {
                        return factory.createEnumValue(classifier)
                    }
                }
                return factory.createErrorValue("Unresolved enum entry: $enumClassId.$name")
            }

            override fun visitEnd() {
                result.add(AnnotationDescriptorImpl(annotationClass.defaultType, arguments, source))
            }

            private fun createConstant(name: Name?, value: Any?): ConstantValue<*> {
                return factory.createConstantValue(value) ?:
                       factory.createErrorValue("Unsupported annotation argument: $name")
            }

            private fun setArgumentValueByName(name: Name, argumentValue: ConstantValue<*>) {
                val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                if (parameter != null) {
                    arguments[parameter] = argumentValue
                }
            }
        }
    }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findClassAcrossModuleDependencies(classId)
               ?: ErrorUtils.createErrorClass(classId.asSingleFqName().asString())
    }
}
