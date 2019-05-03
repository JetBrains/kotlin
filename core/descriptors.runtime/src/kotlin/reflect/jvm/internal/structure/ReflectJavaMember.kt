/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal.structure

import org.jetbrains.kotlin.load.java.structure.JavaMember
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.*

abstract class ReflectJavaMember : ReflectJavaElement(), ReflectJavaAnnotationOwner, ReflectJavaModifierListOwner, JavaMember {
    abstract val member: Member

    override val element: AnnotatedElement get() = member as AnnotatedElement

    override val modifiers: Int get() = member.modifiers

    override val name: Name
        get() = member.name?.let { Name.identifier(it) } ?: SpecialNames.NO_NAME_PROVIDED

    override val containingClass: ReflectJavaClass
        get() = ReflectJavaClass(member.declaringClass)

    protected fun getValueParameters(
        parameterTypes: Array<Type>,
        parameterAnnotations: Array<Array<Annotation>>,
        isVararg: Boolean
    ): List<JavaValueParameter> {
        val result = ArrayList<JavaValueParameter>(parameterTypes.size)
        val names = Java8ParameterNamesLoader.loadParameterNames(member)

        // Skip synthetic parameters such as outer class instance
        val shift = names?.size?.minus(parameterTypes.size) ?: 0

        for (i in parameterTypes.indices) {
            val type = ReflectJavaType.create(parameterTypes[i])
            val name = names?.run {
                getOrNull(i + shift) ?: error("No parameter with index $i+$shift (name=$name type=$type) in $this@ReflectJavaMember")
            }
            val isParamVararg = isVararg && i == parameterTypes.lastIndex
            result.add(ReflectJavaValueParameter(type, parameterAnnotations[i], name, isParamVararg))
        }
        return result
    }

    override fun equals(other: Any?) = other is ReflectJavaMember && member == other.member

    override fun hashCode() = member.hashCode()

    override fun toString() = this::class.java.name + ": " + member
}

private object Java8ParameterNamesLoader {
    class Cache(val getParameters: Method?, val getName: Method?)

    var cache: Cache? = null

    fun buildCache(member: Member): Cache {
        // This should be either j.l.reflect.Method or j.l.reflect.Constructor
        val methodOrConstructorClass = member::class.java

        val getParameters = try {
            methodOrConstructorClass.getMethod("getParameters")
        } catch (e: NoSuchMethodException) {
            return Cache(null, null)
        }

        val parameterClass = methodOrConstructorClass.safeClassLoader.loadClass("java.lang.reflect.Parameter")

        return Cache(getParameters, parameterClass.getMethod("getName"))
    }

    fun loadParameterNames(member: Member): List<String>? {
        var cache = cache
        if (cache == null) {
            cache = buildCache(member)
            this.cache = cache
        }

        val getParameters = cache.getParameters ?: return null
        val getName = cache.getName ?: return null

        return (getParameters(member) as Array<*>).map { param ->
            getName(param) as String
        }
    }
}
