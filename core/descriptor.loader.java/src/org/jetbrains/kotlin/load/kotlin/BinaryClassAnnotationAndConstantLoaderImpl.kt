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

import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.createCompileTimeConstant
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant

public class BinaryClassAnnotationAndConstantLoaderImpl(
        private val module: ModuleDescriptor,
        storageManager: StorageManager,
        kotlinClassFinder: KotlinClassFinder,
        errorReporter: ErrorReporter
) : AbstractBinaryClassAnnotationAndConstantLoader<AnnotationDescriptor, CompileTimeConstant<*>>(
        storageManager, kotlinClassFinder, errorReporter
) {

    override fun loadConstant(desc: String, initializer: Any): CompileTimeConstant<*>? {
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

        val compileTimeConstant = createCompileTimeConstant(
                normalizedValue, canBeUsedInAnnotation = true, isPureIntConstant = true,
                usesVariableAsConstant = true, expectedType = null
        )
        return compileTimeConstant
    }


    override fun loadAnnotation(
            annotationClassId: ClassId,
            result: MutableList<AnnotationDescriptor>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val annotationClass = resolveClass(annotationClassId)

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

            // NOTE: see analogous code in AnnotationDeserializer
            private fun enumEntryValue(enumClassId: ClassId, name: Name): CompileTimeConstant<*> {
                val enumClass = resolveClass(enumClassId)
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
                return createCompileTimeConstant(value, canBeUsedInAnnotation = true, isPureIntConstant = false,
                                                 usesVariableAsConstant = false, expectedType = null)
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

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findClassAcrossModuleDependencies(classId)
               ?: ErrorUtils.createErrorClass(classId.asSingleFqName().asString())
    }
}
