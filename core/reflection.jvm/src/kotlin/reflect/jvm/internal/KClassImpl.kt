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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KotlinReflectionInternalError

class KClassImpl<T : Any>(override val jClass: Class<T>) : KDeclarationContainerImpl(), KClass<T>, KAnnotatedElementImpl {
    val descriptor by ReflectProperties.lazySoft {
        val classId = classId

        val descriptor =
                if (classId.isLocal()) moduleData.localClassResolver.resolveLocalClass(classId)
                else moduleData.module.findClassAcrossModuleDependencies(classId)

        descriptor ?: throw KotlinReflectionInternalError("Class not resolved: $jClass")
    }

    override val annotated: Annotated get() = descriptor

    private val classId: ClassId get() = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

    override val scope: JetScope get() = ChainedScope(
            descriptor, "KClassImpl scope", descriptor.getDefaultType().getMemberScope(), descriptor.getStaticScope()
    )

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() {
            val descriptor = descriptor
            if (descriptor.getKind() == ClassKind.CLASS || descriptor.getKind() == ClassKind.ENUM_CLASS) {
                return descriptor.getConstructors()
            }
            return emptyList()
        }

    override val simpleName: String? get() {
        if (jClass.isAnonymousClass()) return null

        val classId = classId
        return when {
            classId.isLocal() -> calculateLocalClassName(jClass)
            else -> classId.getShortClassName().asString()
        }
    }

    private fun calculateLocalClassName(jClass: Class<*>): String {
        val name = jClass.getSimpleName()
        jClass.getEnclosingMethod()?.let { method ->
            return name.substringAfter(method.getName() + "$")
        }
        jClass.getEnclosingConstructor()?.let { constructor ->
            return name.substringAfter(constructor.getName() + "$")
        }
        return name.substringAfter('$')
    }

    override val qualifiedName: String? get() {
        if (jClass.isAnonymousClass()) return null

        val classId = classId
        return when {
            classId.isLocal() -> null
            else -> classId.asSingleFqName().asString()
        }
    }

    @suppress("UNCHECKED_CAST")
    override val constructors: Collection<KFunction<T>>
        get() = constructorDescriptors.map {
            KFunctionImpl(this, it) as KFunction<T>
        }

    @suppress("UNCHECKED_CAST")
    override val objectInstance: T? by ReflectProperties.lazy {
        val descriptor = descriptor
        if (descriptor.kind != ClassKind.OBJECT) return@lazy null

        val field = if (descriptor.isCompanionObject) {
            jClass.enclosingClass.getDeclaredField(descriptor.name.asString())
        }
        else {
            jClass.getDeclaredField(JvmAbi.INSTANCE_FIELD)
        }
        field.get(null) as T
    }

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String {
        return "class " + classId.let { classId ->
            val packageFqName = classId.getPackageFqName()
            val packagePrefix = if (packageFqName.isRoot()) "" else packageFqName.asString() + "."
            val classSuffix = classId.getRelativeClassName().asString().replace('.', '$')
            packagePrefix + classSuffix
        }
    }
}
