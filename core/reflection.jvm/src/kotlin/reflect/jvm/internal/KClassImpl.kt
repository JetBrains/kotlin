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
import org.jetbrains.kotlin.load.java.reflect.tryLoadClass
import org.jetbrains.kotlin.load.java.structure.reflect.safeClassLoader
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KotlinReflectionInternalError

internal class KClassImpl<T : Any>(override val jClass: Class<T>) : KDeclarationContainerImpl(), KClass<T>, KAnnotatedElementImpl {
    private val descriptor_ = ReflectProperties.lazySoft {
        val classId = classId

        val descriptor =
                if (classId.isLocal) moduleData.deserialization.deserializeClass(classId)
                else moduleData.module.findClassAcrossModuleDependencies(classId)

        descriptor ?: reportUnresolvedClass()
    }

    val descriptor: ClassDescriptor
        get() = descriptor_()

    override val annotated: Annotated get() = descriptor

    private val classId: ClassId get() = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

    internal val memberScope: MemberScope get() = descriptor.defaultType.memberScope

    internal val staticScope: MemberScope get() = descriptor.staticScope

    override val members: Collection<KCallable<*>>
        get() = getMembers(memberScope, declaredOnly = false, nonExtensions = true, extensions = true)
                .plus(getMembers(staticScope, declaredOnly = false, nonExtensions = true, extensions = true))
                .toList()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() {
            val descriptor = descriptor
            if (descriptor.kind == ClassKind.CLASS || descriptor.kind == ClassKind.ENUM_CLASS) {
                return descriptor.constructors
            }
            return emptyList()
        }

    @Suppress("UNCHECKED_CAST")
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
            (nestedClass as ClassDescriptor).toJavaClass() ?: run {
                // If neither a Kotlin class nor a Java class, it must be a built-in
                val classId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(DescriptorUtils.getFqName(nestedClass))
                              ?: throw KotlinReflectionInternalError("Class with no source must be a built-in: $nestedClass")
                val packageName = classId.packageFqName.asString()
                val className = classId.relativeClassName.asString().replace('.', '$')
                // All pseudo-classes like String.Companion must be accessible from the current class loader
                (this as Any).javaClass.safeClassLoader.tryLoadClass("$packageName.$className")
            }
        }.map { KClassImpl(it) }

    @Suppress("UNCHECKED_CAST")
    private val objectInstance_ = ReflectProperties.lazy {
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

    override val objectInstance: T?
        get() = objectInstance_()

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
