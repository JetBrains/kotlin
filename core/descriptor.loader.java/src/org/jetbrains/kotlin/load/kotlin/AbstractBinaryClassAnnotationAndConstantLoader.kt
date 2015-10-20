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

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.index
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.methodImplClassName
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.propertyImplClassName
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.propertySignature
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

public abstract class AbstractBinaryClassAnnotationAndConstantLoader<A : Any, C : Any, T : Any>(
        storageManager: StorageManager,
        private val kotlinClassFinder: KotlinClassFinder,
        private val errorReporter: ErrorReporter
) : AnnotationAndConstantLoader<A, C, T> {
    private val storage = storageManager.createMemoizedFunction<KotlinJvmBinaryClass, Storage<A, C>> {
        kotlinClass ->
        loadAnnotationsAndInitializers(kotlinClass)
    }

    protected abstract fun loadConstant(desc: String, initializer: Any): C?

    protected abstract fun loadAnnotation(
            annotationClassId: ClassId,
            source: SourceElement,
            result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor?

    protected abstract fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): A

    private fun loadAnnotationIfNotSpecial(
            annotationClassId: ClassId,
            source: SourceElement,
            result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (JvmAnnotationNames.isSpecialAnnotation(annotationClassId, true)) return null

        return loadAnnotation(annotationClassId, source, result)
    }

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<A> {
        val classId = nameResolver.getClassId(classProto.fqName)
        val kotlinClass = kotlinClassFinder.findKotlinClass(classId)
        if (kotlinClass == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: KotlinClassFinder had found the
            // class earlier, but it can't now
            errorReporter.reportLoadingError("Kotlin class for loading class annotations is not found: ${classId.asSingleFqName()}", null)
            return listOf()
        }

        val result = ArrayList<A>(1)

        kotlinClass.loadClassAnnotations(object : KotlinJvmBinaryClass.AnnotationVisitor {
            override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                return loadAnnotationIfNotSpecial(classId, source, result)
            }

            override fun visitEnd() {
            }
        })

        return result
    }

    override fun loadCallableAnnotations(container: ProtoContainer, proto: MessageLite, kind: AnnotatedCallableKind): List<T> {
        if (kind == AnnotatedCallableKind.PROPERTY) {
            proto as ProtoBuf.Property

            val nameResolver = container.nameResolver
            val syntheticFunctionSignature = getPropertySignature(proto, nameResolver, container.typeTable, synthetic = true)
            val fieldSignature = getPropertySignature(proto, nameResolver, container.typeTable, field = true)

            val propertyAnnotations = syntheticFunctionSignature?.let { sig ->
                findClassAndLoadMemberAnnotations(container, proto, sig)
            }.orEmpty()

            val fieldAnnotations = fieldSignature?.let { sig ->
                findClassAndLoadMemberAnnotations(container, proto, sig, isStaticFieldInOuter(proto))
            }.orEmpty()

            return loadPropertyAnnotations(propertyAnnotations, fieldAnnotations)
        }

        val signature = getCallableSignature(proto, container.nameResolver, container.typeTable, kind) ?: return emptyList()
        return transformAnnotations(findClassAndLoadMemberAnnotations(container, proto, signature))
    }

    protected abstract fun loadPropertyAnnotations(propertyAnnotations: List<A>, fieldAnnotations: List<A>): List<T>

    protected abstract fun transformAnnotations(annotations: List<A>): List<T>

    private fun findClassAndLoadMemberAnnotations(
            container: ProtoContainer,
            proto: MessageLite,
            signature: MemberSignature,
            isStaticFieldInOuter: Boolean = false
    ): List<A> {
        val kotlinClass = findClassWithAnnotationsAndInitializers(
                container, getImplClassName(proto, container.nameResolver), isStaticFieldInOuter
        )
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading member annotations is not found: ${container.getFqName()}", null)
            return listOf()
        }

        return storage(kotlinClass).memberAnnotations[signature] ?: listOf()
    }

    override fun loadValueParameterAnnotations(
            container: ProtoContainer,
            message: MessageLite,
            kind: AnnotatedCallableKind,
            parameterIndex: Int,
            proto: ProtoBuf.ValueParameter
    ): List<A> {
        val methodSignature = getCallableSignature(message, container.nameResolver, container.typeTable, kind)
        if (methodSignature != null) {
            val index = if (proto.hasExtension(index)) proto.getExtension(index) else parameterIndex
            val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, index)
            return findClassAndLoadMemberAnnotations(container, message, paramSignature)
        }

        return listOf()
    }

    override fun loadExtensionReceiverParameterAnnotations(
            container: ProtoContainer,
            message: MessageLite,
            kind: AnnotatedCallableKind
    ): List<A> {
        val methodSignature = getCallableSignature(message, container.nameResolver, container.typeTable, kind)
        if (methodSignature != null) {
            val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, 0)
            return findClassAndLoadMemberAnnotations(container, message, paramSignature)
        }

        return emptyList()
    }

    override fun loadTypeAnnotations(type: ProtoBuf.Type, nameResolver: NameResolver): List<A> {
        return type.getExtension(JvmProtoBuf.typeAnnotation).map { loadTypeAnnotation(it, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(typeParameter: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<A> {
        return typeParameter.getExtension(JvmProtoBuf.typeParameterAnnotation).map { loadTypeAnnotation(it, nameResolver) }
    }

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Property, expectedType: KotlinType): C? {
        val nameResolver = container.nameResolver
        val signature = getCallableSignature(proto, nameResolver, container.typeTable, AnnotatedCallableKind.PROPERTY) ?: return null

        val kotlinClass = findClassWithAnnotationsAndInitializers(
                container, getImplClassName(proto, nameResolver), isStaticFieldInOuter(proto)
        )
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading property constant is not found: ${container.getFqName()}", null)
            return null
        }

        return storage(kotlinClass).propertyConstants[signature]
    }

    private fun findClassWithAnnotationsAndInitializers(
            container: ProtoContainer, implClassName: Name?, isStaticFieldInOuter: Boolean
    ): KotlinJvmBinaryClass? {
        val (classProto, packageFqName) = container
        return when {
            packageFqName != null -> {
                implClassName?.let { kotlinClassFinder.findKotlinClass(ClassId(packageFqName, it)) }
            }
            classProto != null -> {
                val classId = container.nameResolver.getClassId(classProto.fqName)

                if (implClassName != null) {
                    // TODO: store accurate name for nested traits
                    val implClassId =
                        if (implClassName.asString().endsWith(JvmAbi.DEFAULT_IMPLS_SUFFIX))
                            ClassId(classId.packageFqName, FqName(implClassName.asString().replace(JvmAbi.DEFAULT_IMPLS_SUFFIX, "." + JvmAbi.DEFAULT_IMPLS_CLASS_NAME)), false)
                        else
                            ClassId(classId.packageFqName, implClassName)
                    return kotlinClassFinder.findKotlinClass(implClassId)
                }

                if (isStaticFieldInOuter && classId.isNestedClass) {
                    // Backing fields of properties of a companion object are generated in the outer class
                    return kotlinClassFinder.findKotlinClass(classId.outerClassId)
                }

                kotlinClassFinder.findKotlinClass(classId)
            }
            else -> null
        }
    }

    private fun getImplClassName(proto: MessageLite, nameResolver: NameResolver): Name? =
            when {
                proto is ProtoBuf.Function && proto.hasExtension(methodImplClassName) ->
                    nameResolver.getName(proto.getExtension(methodImplClassName))
                proto is ProtoBuf.Property && proto.hasExtension(propertyImplClassName) ->
                    nameResolver.getName(proto.getExtension(propertyImplClassName))
                else -> null
            }

    private fun isStaticFieldInOuter(proto: MessageLite): Boolean =
            if (proto is ProtoBuf.Property && proto.hasExtension(propertySignature))
                proto.getExtension(propertySignature).let { it.hasField() && it.field.isStaticInOuter }
            else false

    private fun loadAnnotationsAndInitializers(kotlinClass: KotlinJvmBinaryClass): Storage<A, C> {
        val memberAnnotations = HashMap<MemberSignature, MutableList<A>>()
        val propertyConstants = HashMap<MemberSignature, C>()

        kotlinClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString(), desc))
            }

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc)

                if (initializer != null) {
                    val constant = loadConstant(desc, initializer)
                    if (constant != null) {
                        propertyConstants[signature] = constant
                    }
                }
                return MemberAnnotationVisitor(signature)
            }

            inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature), KotlinJvmBinaryClass.MethodAnnotationVisitor {

                override fun visitParameterAnnotation(
                        index: Int, classId: ClassId, source: SourceElement
                ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                    var result = memberAnnotations[paramSignature]
                    if (result == null) {
                        result = ArrayList<A>()
                        memberAnnotations[paramSignature] = result
                    }
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<A>()

                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationIfNotSpecial(classId, source, result)
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

    private fun getPropertySignature(
            proto: ProtoBuf.Property,
            nameResolver: NameResolver,
            typeTable: TypeTable,
            field: Boolean = false,
            synthetic: Boolean = false
    ): MemberSignature? {
        val signature =
                if (proto.hasExtension(propertySignature)) proto.getExtension(propertySignature)
                else return null

        if (field) {
            val (name, desc) = JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable) ?: return null
            return MemberSignature.fromFieldNameAndDesc(name, desc)
        }
        else if (synthetic && signature.hasSyntheticMethod()) {
            return MemberSignature.fromMethod(nameResolver, signature.syntheticMethod)
        }

        return null
    }

    private fun getCallableSignature(
            proto: MessageLite,
            nameResolver: NameResolver,
            typeTable: TypeTable,
            kind: AnnotatedCallableKind
    ): MemberSignature? {
        return when {
            proto is ProtoBuf.Constructor -> {
                MemberSignature.fromMethodNameAndDesc(JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable) ?: return null)
            }
            proto is ProtoBuf.Function -> {
                MemberSignature.fromMethodNameAndDesc(JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable) ?: return null)
            }
            proto is ProtoBuf.Property && proto.hasExtension(propertySignature) -> {
                val signature = proto.getExtension(propertySignature)
                when (kind) {
                    AnnotatedCallableKind.PROPERTY_GETTER -> MemberSignature.fromMethod(nameResolver, signature.getter)
                    AnnotatedCallableKind.PROPERTY_SETTER -> MemberSignature.fromMethod(nameResolver, signature.setter)
                    AnnotatedCallableKind.PROPERTY -> getPropertySignature(proto, nameResolver, typeTable, true, true)
                    else -> null
                }
            }
            else -> null
        }
    }

    private class Storage<A, C>(
            public val memberAnnotations: Map<MemberSignature, List<A>>,
            public val propertyConstants: Map<MemberSignature, C>
    )
}
