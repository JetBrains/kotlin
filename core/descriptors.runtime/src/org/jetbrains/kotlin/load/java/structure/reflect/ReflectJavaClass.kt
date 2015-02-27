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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeSubstitutor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.emptyOrSingletonList
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays

public class ReflectJavaClass(private val klass: Class<*>) : ReflectJavaElement(), ReflectJavaAnnotationOwner, JavaClass {
    override val element: AnnotatedElement get() = klass

    override fun getInnerClasses() = klass.getDeclaredClasses()
            .stream()
            .filterNot {
                // getDeclaredClasses() returns anonymous classes sometimes, for example enums with specialized entries (which are in fact
                // anonymous classes) or in case of a special anonymous class created for the synthetic accessor to a private nested class
                // constructor accessed from the outer class
                it.getSimpleName().isEmpty()
            }
            .map(::ReflectJavaClass)
            .toList()

    override fun getFqName() = klass.fqName

    override fun getOuterClass() = klass.getDeclaringClass()?.let(::ReflectJavaClass)

    override fun getSupertypes(): Collection<JavaClassifierType> {
        val supertype = klass.getGenericSuperclass()
        val superClassName = (supertype as? Class<*>)?.getName()
        val supertypes =
                (if (superClassName == "java.lang.Object") emptyList() else emptyOrSingletonList(supertype)) +
                klass.getGenericInterfaces()
        return supertypes.map(::ReflectJavaClassifierType)
    }

    override fun getMethods() = klass.getDeclaredMethods()
            .stream()
            .filter { method ->
                when {
                    method.isSynthetic() -> false
                    isEnum() -> !isEnumValuesOrValueOf(method)
                    else -> true
                }
            }
            .map(::ReflectJavaMethod)
            .toList()

    private fun isEnumValuesOrValueOf(method: Method): Boolean {
        return when (method.getName()) {
            "values" -> method.getParameterTypes().isEmpty()
            "valueOf" -> Arrays.equals(method.getParameterTypes(), array(javaClass<String>()))
            else -> false
        }
    }

    override fun getFields() = klass.getDeclaredFields()
            .stream()
            .filter { field -> !field.isSynthetic() }
            .map(::ReflectJavaField)
            .toList()

    override fun getConstructors() = klass.getDeclaredConstructors()
            .stream()
            .filter { constructor -> !constructor.isSynthetic() }
            .map(::ReflectJavaConstructor)
            .toList()

    override fun getDefaultType(): ReflectJavaClassifierType = throw UnsupportedOperationException()

    // TODO: drop OriginKind
    override fun getOriginKind() = JavaClass.OriginKind.COMPILED

    override fun createImmediateType(substitutor: JavaTypeSubstitutor): JavaType = throw UnsupportedOperationException()

    override fun getName(): Name = Name.identifier(klass.getSimpleName())

    override fun getTypeParameters() = klass.getTypeParameters().map { ReflectJavaTypeParameter(it) }

    override fun isInterface() = klass.isInterface()
    override fun isAnnotationType() = klass.isAnnotation()
    override fun isEnum() = klass.isEnum()

    override fun isAbstract() = Modifier.isAbstract(klass.getModifiers())
    override fun isStatic() = Modifier.isStatic(klass.getModifiers())
    override fun isFinal() = Modifier.isFinal(klass.getModifiers())

    override fun getVisibility() = calculateVisibility(klass.getModifiers())

    override fun equals(other: Any?) = other is ReflectJavaClass && klass == other.klass

    override fun hashCode() = klass.hashCode()

    override fun toString() = javaClass.getName() + ": " + klass
}
