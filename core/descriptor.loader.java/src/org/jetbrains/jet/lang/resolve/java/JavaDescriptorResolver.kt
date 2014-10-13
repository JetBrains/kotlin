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

package org.jetbrains.jet.lang.resolve.java

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.java.structure.*
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElement

public var PLATFORM_TYPES: Boolean = true

public class JavaDescriptorResolver(public val packageFragmentProvider: LazyJavaPackageFragmentProvider, private val module: ModuleDescriptor) {

    public fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
        return packageFragmentProvider.getClass(javaClass)
    }
}

public fun JavaDescriptorResolver.resolveMethod(method: JavaMethod): FunctionDescriptor? {
    return getContainingScope(method)?.getFunctions(method.getName())?.findByJavaElement(method)
}

public fun JavaDescriptorResolver.resolveConstructor(constructor: JavaConstructor): ConstructorDescriptor? {
    return resolveClass(constructor.getContainingClass())?.getConstructors()?.findByJavaElement(constructor)
}

public fun JavaDescriptorResolver.resolveField(field: JavaField): PropertyDescriptor? {
    return getContainingScope(field)?.getProperties(field.getName())?.findByJavaElement(field) as? PropertyDescriptor
}

private fun JavaDescriptorResolver.getContainingScope(member: JavaMember): JetScope? {
    val containingClass = resolveClass(member.getContainingClass())
    return if (member.isStatic())
        containingClass?.getStaticScope()
    else
        containingClass?.getDefaultType()?.getMemberScope()
}

private fun <T : DeclarationDescriptorWithSource> Collection<T>.findByJavaElement(javaElement: JavaElement): T? {
    return firstOrNull {
        member ->
        (member.getSource() as? JavaSourceElement)?.javaElement == javaElement
    }
}
