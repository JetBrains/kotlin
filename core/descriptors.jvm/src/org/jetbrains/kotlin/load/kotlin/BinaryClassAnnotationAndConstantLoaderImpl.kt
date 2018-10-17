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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.compact
import java.util.*

class BinaryClassAnnotationAndConstantLoaderImpl(
        private val module: ModuleDescriptor,
        private val notFoundClasses: NotFoundClasses,
        storageManager: StorageManager,
        kotlinClassFinder: KotlinClassFinder
) : AbstractBinaryClassAnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>>(
        storageManager, kotlinClassFinder
) {
    private val annotationDeserializer = AnnotationDeserializer(module, notFoundClasses)

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

        return ConstantValueFactory.createConstantValue(normalizedValue)
    }

    override fun transformToUnsignedConstant(constant: ConstantValue<*>): ConstantValue<*>? {
        return when (constant) {
            is ByteValue -> UByteValue(constant.value)
            is ShortValue -> UShortValue(constant.value)
            is IntValue -> UIntValue(constant.value)
            is LongValue -> ULongValue(constant.value)
            else -> constant
        }
    }

    override fun loadAnnotation(
            annotationClassId: ClassId,
            source: SourceElement,
            result: MutableList<AnnotationDescriptor>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val annotationClass = resolveClass(annotationClassId)

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val arguments = HashMap<Name, ConstantValue<*>>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    arguments[name] = createConstant(name, value)
                }
            }

            override fun visitClassLiteral(name: Name, value: ClassLiteralValue) {
                arguments[name] = value.toClassValue() ?:
                        ErrorValue.create("Error value of annotation argument: $name: class ${value.classId.asSingleFqName()} not found")
            }

            override fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name) {
                arguments[name] = EnumValue(enumClassId, enumEntryName)
            }

            override fun visitArray(name: Name): AnnotationArrayArgumentVisitor? {
                return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                    private val elements = ArrayList<ConstantValue<*>>()

                    override fun visit(value: Any?) {
                        elements.add(createConstant(name, value))
                    }

                    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                        elements.add(EnumValue(enumClassId, enumEntryName))
                    }

                    override fun visitClassLiteral(value: ClassLiteralValue) {
                        elements.add(
                            value.toClassValue() ?: ErrorValue.create(
                                "Error array element value of annotation argument: $name: class ${value.classId.asSingleFqName()} not found"
                            )
                        )
                    }

                    override fun visitEnd() {
                        val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                        if (parameter != null) {
                            arguments[name] = ConstantValueFactory.createArrayValue(elements.compact(), parameter.type)
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
                        arguments[name] = AnnotationValue(list.single())
                    }
                }
            }

            override fun visitEnd() {
                result.add(AnnotationDescriptorImpl(annotationClass.defaultType, arguments, source))
            }

            private fun createConstant(name: Name?, value: Any?): ConstantValue<*> {
                return ConstantValueFactory.createConstantValue(value)
                       ?: ErrorValue.create("Unsupported annotation argument: $name")
            }
        }
    }

    private fun ClassLiteralValue.toClassValue(): KClassValue? =
        module.findClassAcrossModuleDependencies(classId)?.let { classDescriptor ->
            var currentType = classDescriptor.defaultType
            for (i in 0 until arrayNestedness) {
                val nextWrappedType =
                    (if (i == 0) module.builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(currentType) else null)
                        ?: module.builtIns.getArrayType(Variance.INVARIANT, currentType)
                currentType = nextWrappedType
            }
            val kClass = resolveClass(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.kClass.toSafe()))
            val arguments = listOf(TypeProjectionImpl(currentType))
            val type = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClass, arguments)
            KClassValue(type)
        }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
}
