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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.structure.reflect.functionClassArity
import org.jetbrains.kotlin.load.java.structure.reflect.wrapperByPrimitive
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import kotlin.jvm.internal.TypeIntrinsics
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KDeclarationContainerImpl.MemberBelonginess.DECLARED
import kotlin.reflect.jvm.internal.KDeclarationContainerImpl.MemberBelonginess.INHERITED

internal class KClassImpl<T : Any>(override val jClass: Class<T>) :
        KDeclarationContainerImpl(), KClass<T>, KClassifierImpl, KAnnotatedElementImpl {
    inner class Data : KDeclarationContainerImpl.Data() {
        val descriptor: ClassDescriptor by ReflectProperties.lazySoft {
            val classId = classId
            val moduleData = data().moduleData

            val descriptor =
                    if (classId.isLocal) moduleData.deserialization.deserializeClass(classId)
                    else moduleData.module.findClassAcrossModuleDependencies(classId)

            descriptor ?: reportUnresolvedClass()
        }

        @Suppress("UNCHECKED_CAST")
        val objectInstance: T? by ReflectProperties.lazy {
            val descriptor = descriptor
            if (descriptor.kind != ClassKind.OBJECT) return@lazy null

            val field = if (descriptor.isCompanionObject && !CompanionObjectMapping.isMappedIntrinsicCompanionObject(descriptor)) {
                jClass.enclosingClass.getDeclaredField(descriptor.name.asString())
            }
            else {
                jClass.getDeclaredField(JvmAbi.INSTANCE_FIELD)
            }
            field.get(null) as T
        }

        val declaredNonStaticMembers: Sequence<KCallableImpl<*>>
                by ReflectProperties.lazySoft { getMembers(memberScope, DECLARED) }
        val declaredStaticMembers: Sequence<KCallableImpl<*>>
                by ReflectProperties.lazySoft { getMembers(staticScope, DECLARED) }
        val inheritedNonStaticMembers: Sequence<KCallableImpl<*>>
                by ReflectProperties.lazySoft { getMembers(memberScope, INHERITED) }
        val inheritedStaticMembers: Sequence<KCallableImpl<*>>
                by ReflectProperties.lazySoft { getMembers(staticScope, INHERITED) }

        val allNonStaticMembers: Sequence<KCallableImpl<*>> get() = declaredNonStaticMembers + inheritedNonStaticMembers
        val allStaticMembers: Sequence<KCallableImpl<*>> get() = declaredStaticMembers + inheritedStaticMembers
        val declaredMembers: Sequence<KCallableImpl<*>> get() = declaredNonStaticMembers + declaredStaticMembers

        val allMembers: Collection<KCallable<*>> by ReflectProperties.lazySoft {
            (allNonStaticMembers + allStaticMembers).toList()
        }
    }

    val data = ReflectProperties.lazy { Data() }

    override val descriptor: ClassDescriptor get() = data().descriptor

    override val annotated: Annotated get() = descriptor

    private val classId: ClassId get() = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

    internal val memberScope: MemberScope get() = descriptor.defaultType.memberScope

    internal val staticScope: MemberScope get() = descriptor.staticScope

    override val members: Collection<KCallable<*>> get() = data().allMembers

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() {
            val descriptor = descriptor
            if (descriptor.kind == ClassKind.CLASS || descriptor.kind == ClassKind.ENUM_CLASS) {
                return descriptor.constructors
            }
            return emptyList()
        }

    override fun getProperties(name: Name): Collection<PropertyDescriptor> =
            (memberScope.getContributedVariables(name, NoLookupLocation.FROM_REFLECTION) +
             staticScope.getContributedVariables(name, NoLookupLocation.FROM_REFLECTION))

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> =
            memberScope.getContributedFunctions(name, NoLookupLocation.FROM_REFLECTION) +
            staticScope.getContributedFunctions(name, NoLookupLocation.FROM_REFLECTION)

    override val simpleName: String? get() {
        if (jClass.isAnonymousClass) return null

        val classId = classId
        return when {
            classId.isLocal -> calculateLocalClassName(jClass)
            else -> classId.shortClassName.asString()
        }
    }

    private fun calculateLocalClassName(jClass: Class<*>): String {
        val name = jClass.simpleName
        jClass.enclosingMethod?.let { method ->
            return name.substringAfter(method.name + "$")
        }
        jClass.enclosingConstructor?.let { constructor ->
            return name.substringAfter(constructor.name + "$")
        }
        return name.substringAfter('$')
    }

    override val qualifiedName: String? get() {
        if (jClass.isAnonymousClass) return null

        val classId = classId
        return when {
            classId.isLocal -> null
            else -> classId.asSingleFqName().asString()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override val constructors: Collection<KFunction<T>>
        get() = constructorDescriptors.map {
            KFunctionImpl(this, it) as KFunction<T>
        }

    override val nestedClasses: Collection<KClass<*>>
        get() = descriptor.unsubstitutedInnerClassesScope.getContributedDescriptors().filterNot(DescriptorUtils::isEnumEntry).mapNotNull {
            nestedClass ->
            val jClass = (nestedClass as ClassDescriptor).toJavaClass()
            jClass?.let { KClassImpl(it) }
        }

    override val objectInstance: T? get() = data().objectInstance

    override fun isInstance(value: Any?): Boolean {
        // TODO: use Kotlin semantics for mutable/read-only collections once KT-11754 is supported (see TypeIntrinsics)
        jClass.functionClassArity?.let { arity ->
            return TypeIntrinsics.isFunctionOfArity(value, arity)
        }
        return (jClass.wrapperByPrimitive ?: jClass).isInstance(value)
    }

    override val typeParameters: List<KTypeParameter>
        get() = descriptor.declaredTypeParameters.map(::KTypeParameterImpl)

    override val supertypes: List<KType>
        get() = descriptor.typeConstructor.supertypes.map { kotlinType ->
            KTypeImpl(kotlinType) {
                val superClass = kotlinType.constructor.declarationDescriptor
                if (superClass !is ClassDescriptor) throw KotlinReflectionInternalError("Supertype not a class: $superClass")

                val superJavaClass = superClass.toJavaClass()
                                     ?: throw KotlinReflectionInternalError("Unsupported superclass of $this: $superClass")

                if (jClass.superclass == superJavaClass) {
                    jClass.genericSuperclass
                }
                else {
                    val index = jClass.interfaces.indexOf(superJavaClass)
                    if (index < 0) throw KotlinReflectionInternalError("No superclass of $this in Java reflection for $superClass")
                    jClass.genericInterfaces[index]
                }
            }
        }

    override val visibility: KVisibility?
        get() = descriptor.visibility.toKVisibility()

    override val isFinal: Boolean
        get() = descriptor.modality == Modality.FINAL

    override val isOpen: Boolean
        get() = descriptor.modality == Modality.OPEN

    override val isAbstract: Boolean
        get() = descriptor.modality == Modality.ABSTRACT

    override val isSealed: Boolean
        get() = descriptor.modality == Modality.SEALED

    override val isData: Boolean
        get() = descriptor.isData

    override val isInner: Boolean
        get() = descriptor.isInner

    override val isCompanion: Boolean
        get() = descriptor.isCompanionObject

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && javaObjectType == other.javaObjectType

    override fun hashCode(): Int =
            javaObjectType.hashCode()

    override fun toString(): String {
        return "class " + classId.let { classId ->
            val packageFqName = classId.packageFqName
            val packagePrefix = if (packageFqName.isRoot) "" else packageFqName.asString() + "."
            val classSuffix = classId.relativeClassName.asString().replace('.', '$')
            packagePrefix + classSuffix
        }
    }

    private fun reportUnresolvedClass(): Nothing {
        val kind = ReflectKotlinClass.create(jClass)?.classHeader?.kind
        when (kind) {
            KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                throw UnsupportedOperationException(
                        "Packages and file facades are not yet supported in Kotlin reflection. " +
                        "Meanwhile please use Java reflection to inspect this class: $jClass"
                )
            }
            KotlinClassHeader.Kind.SYNTHETIC_CLASS -> {
                throw UnsupportedOperationException(
                        "This class is an internal synthetic class generated by the Kotlin compiler, such as an anonymous class " +
                        "for a lambda, a SAM wrapper, a callable reference, etc. It's not a Kotlin class or interface, so the reflection " +
                        "library has no idea what declarations does it have. Please use Java reflection to inspect this class: $jClass"
                )
            }
            KotlinClassHeader.Kind.UNKNOWN -> {
                // Should not happen since ABI-related exception must have happened earlier
                throw KotlinReflectionInternalError("Unknown class: $jClass (kind = $kind)")
            }
            KotlinClassHeader.Kind.CLASS, null -> {
                // Should not happen since a proper Kotlin- or Java-class must have been resolved
                throw KotlinReflectionInternalError("Unresolved class: $jClass")
            }
        }
    }
}
