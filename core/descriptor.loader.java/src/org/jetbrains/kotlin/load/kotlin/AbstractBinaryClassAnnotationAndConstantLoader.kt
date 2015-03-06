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

import org.jetbrains.kotlin.name.ClassId
import java.util.ArrayList
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.*
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.load.java.components.ErrorReporter
import org.jetbrains.kotlin.name.FqName
import java.util.HashMap
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.types.JetType

public abstract class AbstractBinaryClassAnnotationAndConstantLoader<A : Any, C : Any>(
        storageManager: StorageManager,
        private val kotlinClassFinder: KotlinClassFinder,
        private val errorReporter: ErrorReporter
) : AnnotationAndConstantLoader<A, C> {
    private val storage = storageManager.createMemoizedFunction<KotlinJvmBinaryClass, Storage<A, C>> {
        kotlinClass ->
        loadAnnotationsAndInitializers(kotlinClass)
    }

    protected abstract fun loadConstant(desc: String, initializer: Any): C?

    protected abstract fun loadAnnotation(
            annotationClassId: ClassId,
            result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor?

    private fun loadAnnotationIfNotSpecial(
            annotationClassId: ClassId,
            result: MutableList<A>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (JvmAnnotationNames.isSpecialAnnotation(annotationClassId, true)) return null

        return loadAnnotation(annotationClassId, result)
    }

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<A> {
        val classId = nameResolver.getClassId(classProto.getFqName())
        val kotlinClass = kotlinClassFinder.findKotlinClass(classId)
        if (kotlinClass == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: KotlinClassFinder had found the
            // class earlier, but it can't now
            errorReporter.reportLoadingError("Kotlin class for loading class annotations is not found: ${classId.asSingleFqName()}", null)
            return listOf()
        }

        val result = ArrayList<A>(1)

        kotlinClass.loadClassAnnotations(object : KotlinJvmBinaryClass.AnnotationVisitor {
            override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                return loadAnnotationIfNotSpecial(classId, result)
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
    ): List<A> {
        val signature = getCallableSignature(proto, nameResolver, kind) ?: return listOf()
        return findClassAndLoadMemberAnnotations(container, proto, nameResolver, kind, signature)
    }

    private fun findClassAndLoadMemberAnnotations(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind,
            signature: MemberSignature
    ): List<A> {
        val kotlinClass = findClassWithAnnotationsAndInitializers(container, proto, nameResolver, kind)
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading member annotations is not found: ${container.getFqName(nameResolver)}", null)
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
    ): List<A> {
        val methodSignature = getCallableSignature(callable, nameResolver, kind)
        if (methodSignature != null) {
            if (proto.hasExtension(index)) {
                val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(methodSignature, proto.getExtension(index))
                return findClassAndLoadMemberAnnotations(container, callable, nameResolver, kind, paramSignature)
            }
        }

        return listOf()
    }

    override fun loadPropertyConstant(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            expectedType: JetType
    ): C? {
        val signature = getCallableSignature(proto, nameResolver, AnnotatedCallableKind.PROPERTY) ?: return null

        val kotlinClass = findClassWithAnnotationsAndInitializers(container, proto, nameResolver, AnnotatedCallableKind.PROPERTY)
        if (kotlinClass == null) {
            errorReporter.reportLoadingError("Kotlin class for loading property constant is not found: ${container.getFqName(nameResolver)}", null)
            return null
        }

        return storage(kotlinClass).propertyConstants[signature]
    }

    private fun findClassWithAnnotationsAndInitializers(
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
            // Backing fields of properties of a default object are generated in the outer class
            return kotlinClassFinder.findKotlinClass(classId.getOuterClassId())
        }
        else if (classKind == ProtoBuf.Class.Kind.TRAIT && annotatedCallableKind == AnnotatedCallableKind.PROPERTY) {
            if (proto.hasExtension(implClassName)) {
                val parentPackageFqName = classId.getPackageFqName()
                val tImplName = nameResolver.getName(proto.getExtension(implClassName))
                // TODO: store accurate name for nested traits
                return kotlinClassFinder.findKotlinClass(ClassId(parentPackageFqName, tImplName))
            }
            return null
        }

        return kotlinClassFinder.findKotlinClass(classId)
    }

    private fun findPackagePartClass(
            packageFqName: FqName,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver
    ): KotlinJvmBinaryClass? {
        if (proto.hasExtension(implClassName)) {
            return kotlinClassFinder.findKotlinClass(ClassId(packageFqName, nameResolver.getName(proto.getExtension(implClassName))))
        }
        return null
    }

    private fun isStaticFieldInOuter(proto: ProtoBuf.Callable): Boolean {
        if (!proto.hasExtension(propertySignature)) return false
        val propertySignature = proto.getExtension(propertySignature)
        return propertySignature.hasField() && propertySignature.getField().getIsStaticInOuter()
    }

    private fun loadAnnotationsAndInitializers(kotlinClass: KotlinJvmBinaryClass): Storage<A, C> {
        val memberAnnotations = HashMap<MemberSignature, MutableList<A>>()
        val propertyConstants = HashMap<MemberSignature, C>()

        kotlinClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString() + desc))
            }

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                val signature = MemberSignature.fromFieldNameAndDesc(name, desc)

                if (initializer != null) {
                    val constant = loadConstant(desc, initializer)
                    if (constant != null) {
                        propertyConstants[signature] = constant
                    }
                }
                return MemberAnnotationVisitor(signature)
            }

            inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature), KotlinJvmBinaryClass.MethodAnnotationVisitor {

                override fun visitParameterAnnotation(index: Int, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                    var result = memberAnnotations[paramSignature]
                    if (result == null) {
                        result = ArrayList<A>()
                        memberAnnotations[paramSignature] = result
                    }
                    return loadAnnotationIfNotSpecial(classId, result)
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<A>()

                override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationIfNotSpecial(classId, result)
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

    private class Storage<A, C>(
            public val memberAnnotations: Map<MemberSignature, List<A>>,
            public val propertyConstants: Map<MemberSignature, C>
    )
}

private fun getCallableSignature(
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
