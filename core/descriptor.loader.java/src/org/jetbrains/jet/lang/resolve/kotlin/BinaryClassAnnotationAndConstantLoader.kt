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

import org.jetbrains.jet.descriptors.serialization.*
import org.jetbrains.jet.descriptors.serialization.descriptors.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.annotations.*
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf.*
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.javaClassIdToKotlinClassId
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.kotlinClassIdToJavaClassId
import org.jetbrains.jet.storage.StorageManager
import kotlin.platform.platformStatic

import java.util.*

public class BinaryClassAnnotationAndConstantLoader(
        private val module: ModuleDescriptor,
        storageManager: StorageManager,
        private val kotlinClassFinder: KotlinClassFinder,
        private val errorReporter: ErrorReporter
) : AnnotationAndConstantLoader {

    private val storage = storageManager.createMemoizedFunction<KotlinJvmBinaryClass, Storage>{
        kotlinClass ->
        loadAnnotationsAndInitializers(kotlinClass)
    }

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

        return storage(kotlinClass).memberAnnotations[signature] ?: listOf()
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
            if (proto.hasExtension(index)) {
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

    override fun loadPropertyConstant(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind
    ): CompileTimeConstant<*>? {
        val signature = getCallableSignature(proto, nameResolver, kind) ?: return null

        val kotlinClass = findClassWithAnnotationsAndInitializers(container, proto, nameResolver, kind)
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading property constant is not found: $container", null)
            return null
        }

        return storage(kotlinClass).propertyConstants[signature]
    }


    protected fun findClassWithAnnotationsAndInitializers(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            annotatedCallableKind: AnnotatedCallableKind
    ): KotlinJvmBinaryClass? {
        val packageFqName = container.packageFqName
        if (packageFqName != null) {
            return findPackagePartClass(packageFqName, proto, nameResolver)
        }
        val classProto = container.classProto!!
        val classKind = Flags.CLASS_KIND[classProto.getFlags()]
        val classId = nameResolver.getClassId(classProto.getFqName())
        if (classKind == ProtoBuf.Class.Kind.CLASS_OBJECT && isStaticFieldInOuter(proto)) {
            // Backing fields of properties of a class object are generated in the outer class
            return findKotlinClassById(classId.getOuterClassId())
        }
        else if (classKind == ProtoBuf.Class.Kind.TRAIT && annotatedCallableKind == AnnotatedCallableKind.PROPERTY) {
            if (proto.hasExtension(implClassName)) {
                val parentPackageFqName = classId.getPackageFqName()
                val tImplName = nameResolver.getName(proto.getExtension(implClassName))
                // TODO: store accurate name for nested traits
                return findKotlinClassById(ClassId(parentPackageFqName, tImplName))
            }
            return null
        }

        return findKotlinClassById(classId)
    }

    private fun findPackagePartClass(
            packageFqName: FqName,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver
    ): KotlinJvmBinaryClass? {
        if (proto.hasExtension(implClassName)) {
            return findKotlinClassById(ClassId(packageFqName, nameResolver.getName(proto.getExtension(implClassName))))
        }
        return null
    }

    protected fun findKotlinClassById(classId: ClassId): KotlinJvmBinaryClass? {
        return kotlinClassFinder.findKotlinClass(kotlinClassIdToJavaClassId(classId))
    }

    private fun isStaticFieldInOuter(proto: ProtoBuf.Callable): Boolean {
        if (!proto.hasExtension(propertySignature)) return false
        val propertySignature = proto.getExtension(propertySignature)
        return propertySignature.hasField() && propertySignature.getField().getIsStaticInOuter()
    }

    fun getCallableSignature(
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind
    ): MemberSignature? {
        val deserializer = SignatureDeserializer(nameResolver)
        when (kind) {
            AnnotatedCallableKind.FUNCTION -> if (proto.hasExtension(methodSignature)) {
                return deserializer.methodSignature(proto.getExtension(methodSignature))
            }
            AnnotatedCallableKind.PROPERTY_GETTER -> if (proto.hasExtension(propertySignature)) {
                return deserializer.methodSignature(proto.getExtension(propertySignature).getGetter())
            }
            AnnotatedCallableKind.PROPERTY_SETTER -> if (proto.hasExtension(propertySignature)) {
                return deserializer.methodSignature(proto.getExtension(propertySignature).getSetter())
            }
            AnnotatedCallableKind.PROPERTY -> if (proto.hasExtension(propertySignature)) {
                val propertySignature = proto.getExtension(propertySignature)

                if (propertySignature.hasField()) {
                    val field = propertySignature.getField()
                    val type = deserializer.typeDescriptor(field.getType())
                    val name = nameResolver.getName(field.getName())
                    return MemberSignature.fromFieldNameAndDesc(name, type)
                }
                else if (propertySignature.hasSyntheticMethod()) {
                    return deserializer.methodSignature(propertySignature.getSyntheticMethod())
                }
            }
        }
        return null
    }

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
                    return BinaryClassAnnotationAndConstantLoader.resolveAnnotation(classId, result, module)
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<AnnotationDescriptor>()

                override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return BinaryClassAnnotationAndConstantLoader.resolveAnnotation(classId, result, module)
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

    class Storage(
            public val memberAnnotations: Map<MemberSignature, List<AnnotationDescriptor>>,
            public val propertyConstants: Map<MemberSignature, CompileTimeConstant<*>>
    ) {
        class object {
            public val EMPTY: Storage = Storage(mapOf(), mapOf())
        }
    }
}
