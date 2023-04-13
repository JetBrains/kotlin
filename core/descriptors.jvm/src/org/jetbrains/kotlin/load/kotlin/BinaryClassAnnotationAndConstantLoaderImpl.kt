/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.compact

class BinaryClassAnnotationAndConstantLoaderImpl(
    private val module: ModuleDescriptor,
    private val notFoundClasses: NotFoundClasses,
    storageManager: StorageManager,
    kotlinClassFinder: KotlinClassFinder
) : AbstractBinaryClassAnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>>(
    storageManager, kotlinClassFinder
) {
    private val annotationDeserializer = AnnotationDeserializer(module, notFoundClasses)

    override var jvmMetadataVersion: JvmMetadataVersion = JvmMetadataVersion.INSTANCE

    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationDescriptor =
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
        } else {
            initializer
        }

        return ConstantValueFactory.createConstantValue(normalizedValue, module)
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

        return object : AbstractAnnotationArgumentVisitor() {
            private val arguments = HashMap<Name, ConstantValue<*>>()

            override fun visitConstantValue(name: Name?, value: ConstantValue<*>) {
                if (name != null) arguments[name] = value
            }

            override fun visitArrayValue(name: Name?, elements: ArrayList<ConstantValue<*>>) {
                if (name == null) return
                val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                if (parameter != null) {
                    arguments[name] = ConstantValueFactory.createArrayValue(elements.compact(), parameter.type)
                } else if (isImplicitRepeatableContainer(annotationClassId) && name.asString() == "value") {
                    // In case this is an implicit repeatable annotation container, its class descriptor can't be resolved by the
                    // frontend, so we'd like to flatten its value and add repeated annotations to the list.
                    // E.g. if we see `@Foo.Container(@Foo(1), @Foo(2))` in the bytecode on some declaration where `Foo` is some
                    // Kotlin-repeatable annotation, we want to read annotations on that declaration as a list `[@Foo(1), @Foo(2)]`.
                    elements.filterIsInstance<AnnotationValue>().mapTo(result, AnnotationValue::value)
                }
            }

            override fun visitEnd() {
                // Do not load the @java.lang.annotation.Repeatable annotation instance generated automatically by the compiler for
                // Kotlin-repeatable annotation classes. Otherwise the reference to the implicit nested "Container" class cannot be
                // resolved, since that class is only generated in the backend, and is not visible to the frontend.
                if (isRepeatableWithImplicitContainer(annotationClassId, arguments)) return

                // Do not load the implicit repeatable annotation container entry. The contents of its "value" argument have been flattened
                // and added to the result already, see `visitArray`.
                if (isImplicitRepeatableContainer(annotationClassId)) return

                result.add(AnnotationDescriptorImpl(annotationClass.defaultType, arguments, source))
            }
        }
    }

    override fun loadAnnotationMethodDefaultValue(
        annotationClass: KotlinJvmBinaryClass,
        methodSignature: MemberSignature,
        visitResult: (ConstantValue<*>) -> Unit
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        return object : AbstractAnnotationArgumentVisitor() {
            private var defaultValue: ConstantValue<*>? = null

            override fun visitConstantValue(name: Name?, value: ConstantValue<*>) {
                defaultValue = value
            }

            override fun visitArrayValue(name: Name?, elements: ArrayList<ConstantValue<*>>) {
                defaultValue = ArrayValue(elements.compact()) { moduleDescriptor ->
                    guessArrayType(moduleDescriptor)
                }
            }

            override fun visitEnd() {
                defaultValue?.let(visitResult)
            }

            private fun guessArrayType(
                moduleDescriptor: ModuleDescriptor
            ): KotlinType {
                val elementDesc = methodSignature.signature.substringAfterLast(')').removePrefix("[")
                // Some fast-path guesses
                JvmPrimitiveType.getByDesc(elementDesc)
                    ?.let { return moduleDescriptor.builtIns.getPrimitiveArrayKotlinType(it.primitiveType) }
                if (elementDesc == "Ljava/lang/String;") return moduleDescriptor.builtIns.getArrayType(
                    Variance.INVARIANT,
                    moduleDescriptor.builtIns.stringType
                )
                // Slow path resolving @JvmName
                val propertiesNames = moduleDescriptor.findNonGenericClassAcrossDependencies(annotationClass.classId, notFoundClasses)
                    .unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<PropertyDescriptor>()
                    .filter { prop ->
                        val name = prop.getter?.let { DescriptorUtils.getJvmName(it) ?: prop.name.asString() }
                        name == methodSignature.signature.substringBefore('(')
                    }
                val requiredProp = propertiesNames.singleOrNull()
                    ?: error("Signature ${methodSignature.signature} does not belong to class ${annotationClass.classId} or multiple duplicates found")
                return requiredProp.type
            }
        }
    }

    private abstract inner class AbstractAnnotationArgumentVisitor : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        abstract fun visitConstantValue(name: Name?, value: ConstantValue<*>)
        abstract override fun visitEnd()
        abstract fun visitArrayValue(name: Name?, elements: ArrayList<ConstantValue<*>>)

        override fun visit(name: Name?, value: Any?) {
            visitConstantValue(name, createConstant(name, value))
        }

        override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
            visitConstantValue(name, KClassValue(value))
        }

        override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
            visitConstantValue(name, EnumValue(enumClassId, enumEntryName))
        }

        override fun visitArray(name: Name?): AnnotationArrayArgumentVisitor? {
            return object : AnnotationArrayArgumentVisitor {
                private val elements = ArrayList<ConstantValue<*>>()

                override fun visit(value: Any?) {
                    elements.add(createConstant(name, value))
                }

                override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                    elements.add(EnumValue(enumClassId, enumEntryName))
                }

                override fun visitClassLiteral(value: ClassLiteralValue) {
                    elements.add(KClassValue(value))
                }

                override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val list = ArrayList<AnnotationDescriptor>()
                    val visitor = loadAnnotation(classId, SourceElement.NO_SOURCE, list)!!
                    return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                        override fun visitEnd() {
                            visitor.visitEnd()
                            elements.add(AnnotationValue(list.single()))
                        }
                    }
                }

                override fun visitEnd() {
                    visitArrayValue(name, elements)
                }
            }
        }

        override fun visitAnnotation(name: Name?, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
            val list = ArrayList<AnnotationDescriptor>()
            val visitor = loadAnnotation(classId, SourceElement.NO_SOURCE, list)!!
            return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                override fun visitEnd() {
                    visitor.visitEnd()
                    visitConstantValue(name, AnnotationValue(list.single()))
                }
            }
        }
    }

    private fun createConstant(name: Name?, value: Any?): ConstantValue<*> {
        return ConstantValueFactory.createConstantValue(value, module)
            ?: ErrorValue.create("Unsupported annotation argument: $name")
    }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
}

// Note: this function is needed because we cannot pass JvmMetadataVersion
// directly to the BinaryClassAnnotationAndConstantLoaderImpl constructor.
// This constructor is used by dependency injection.
fun createBinaryClassAnnotationAndConstantLoader(
    module: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    storageManager: StorageManager,
    kotlinClassFinder: KotlinClassFinder,
    jvmMetadataVersion: JvmMetadataVersion
): BinaryClassAnnotationAndConstantLoaderImpl = BinaryClassAnnotationAndConstantLoaderImpl(
    module, notFoundClasses, storageManager, kotlinClassFinder
).apply {
    this.jvmMetadataVersion = jvmMetadataVersion
}
