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

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.implClassName
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.index
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.methodSignature
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.propertySignature
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.JetType
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

    override fun loadCallableAnnotations(container: ProtoContainer, proto: ProtoBuf.Callable, kind: AnnotatedCallableKind): List<T> {
        val nameResolver = container.nameResolver
        if (kind == AnnotatedCallableKind.PROPERTY) {
            val syntheticFunctionSignature = getPropertySignature(proto, nameResolver, synthetic = true)
            val fieldSignature = getPropertySignature(proto, nameResolver, field = true)

            val propertyAnnotations = syntheticFunctionSignature?.let { sig ->
                findClassAndLoadMemberAnnotations(container, proto, sig)
            } ?: listOf()

            val fieldAnnotations = fieldSignature?.let { sig ->
                findClassAndLoadMemberAnnotations(container, proto, sig, isStaticFieldInOuter(proto))
            } ?: listOf()

            return loadPropertyAnnotations(propertyAnnotations, fieldAnnotations)
        }
        val signature = getCallableSignature(proto, nameResolver, kind) ?: return listOf()
        return transformAnnotations(findClassAndLoadMemberAnnotations(container, proto, signature))
    }

    protected abstract fun loadPropertyAnnotations(propertyAnnotations: List<A>, fieldAnnotations: List<A>): List<T>

    protected abstract fun transformAnnotations(annotations: List<A>): List<T>

    private fun findClassAndLoadMemberAnnotations(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
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
            callable: ProtoBuf.Callable,
            kind: AnnotatedCallableKind,
            parameterIndex: Int,
            proto: ProtoBuf.ValueParameter
    ): List<A> {
        val methodSignature = getCallableSignature(callable, container.nameResolver, kind)
        if (methodSignature != null) {
            val index = if (proto.hasExtension(index)) proto.getExtension(index) else parameterIndex
            val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, index)
            return findClassAndLoadMemberAnnotations(container, callable, paramSignature)
        }

        return listOf()
    }

    override fun loadExtensionReceiverParameterAnnotations(
            container: ProtoContainer,
            callable: ProtoBuf.Callable,
            kind: AnnotatedCallableKind
    ): List<A> {
        if (!callable.hasReceiverType()) return emptyList()
        val methodSignature = getCallableSignature(callable, container.nameResolver, kind)
        if (methodSignature != null) {
            val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, 0)
            return findClassAndLoadMemberAnnotations(container, callable, paramSignature)
        }

        return emptyList()
    }

    override fun loadTypeAnnotations(type: ProtoBuf.Type, nameResolver: NameResolver): List<A> {
        return type.getExtension(JvmProtoBuf.typeAnnotation).map { loadTypeAnnotation(it, nameResolver) }
    }

    override fun loadPropertyConstant(container: ProtoContainer, proto: ProtoBuf.Callable, expectedType: JetType): C? {
        val nameResolver = container.nameResolver
        val signature = getCallableSignature(proto, nameResolver, AnnotatedCallableKind.PROPERTY) ?: return null

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
                    return kotlinClassFinder.findKotlinClass(ClassId(classId.packageFqName, implClassName))
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

    private fun getImplClassName(proto: ProtoBuf.Callable, nameResolver: NameResolver): Name? {
        return if (proto.hasExtension(implClassName)) nameResolver.getName(proto.getExtension(implClassName)) else null
    }

    private fun isStaticFieldInOuter(proto: ProtoBuf.Callable): Boolean {
        return proto.hasExtension(propertySignature) &&
               proto.getExtension(propertySignature).let { it.hasField() && it.field.isStaticInOuter }
    }

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
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            field: Boolean = false,
            synthetic: Boolean = false
    ): MemberSignature? {
        if (!proto.hasExtension(propertySignature)) return null

        val signature = proto.getExtension(propertySignature)

        if (field && signature.hasField()) {
            return MemberSignature.fromFieldNameAndDesc(
                    nameResolver.getString(signature.field.name),
                    nameResolver.getString(signature.field.desc)
            )
        }
        else if (synthetic && signature.hasSyntheticMethod()) {
            return MemberSignature.fromMethod(nameResolver, signature.syntheticMethod)
        }

        return null
    }

    private fun getCallableSignature(
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind
    ): MemberSignature? {
        when (kind) {
            AnnotatedCallableKind.FUNCTION -> if (proto.hasExtension(methodSignature)) {
                return MemberSignature.fromMethod(nameResolver, proto.getExtension(methodSignature))
            }
            AnnotatedCallableKind.PROPERTY_GETTER -> if (proto.hasExtension(propertySignature)) {
                return MemberSignature.fromMethod(nameResolver, proto.getExtension(propertySignature).getter)
            }
            AnnotatedCallableKind.PROPERTY_SETTER -> if (proto.hasExtension(propertySignature)) {
                return MemberSignature.fromMethod(nameResolver, proto.getExtension(propertySignature).setter)
            }
            AnnotatedCallableKind.PROPERTY -> return getPropertySignature(proto, nameResolver, true, true)
        }
        return null
    }

    private class Storage<A, C>(
            public val memberAnnotations: Map<MemberSignature, List<A>>,
            public val propertyConstants: Map<MemberSignature, C>
    )
}
