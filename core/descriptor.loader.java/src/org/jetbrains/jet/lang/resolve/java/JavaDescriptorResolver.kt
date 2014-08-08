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

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.structure.JavaField
import org.jetbrains.jet.lang.resolve.java.structure.JavaMember
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElement
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithSource

public class JavaDescriptorResolver(public val packageFragmentProvider: LazyJavaPackageFragmentProvider, private val module: ModuleDescriptor) {
    public fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
        return packageFragmentProvider.getClass(javaClass)
    }

    public fun getPackageFragment(javaClass: JavaClass): PackageFragmentDescriptor? {
        return packageFragmentProvider.getPackageFragment(javaClass)
    }
}

public fun JavaDescriptorResolver.resolveMethod(method: JavaMethod): FunctionDescriptor? {
    val functions = if (method.isConstructor())
                        resolveClass(method.getContainingClass())?.getConstructors()
                    else
                        getContainingScope(method)?.getFunctions(method.getName())

    return functions?.findByJavaElement(method)
}

public fun JavaDescriptorResolver.resolveField(field: JavaField): PropertyDescriptor? {
    return getContainingScope(field)?.getProperties(field.getName())?.findByJavaElement(field) as? PropertyDescriptor
}

private fun JavaDescriptorResolver.getContainingScope(method: JavaMember): JetScope? {
    val containingClass = method.getContainingClass()
    return if (method.isStatic())
        getPackageFragment(containingClass)?.getMemberScope()
    else
        resolveClass(containingClass)?.getDefaultType()?.getMemberScope()
}

private fun <T : DeclarationDescriptorWithSource> Collection<T>.findByJavaElement(javaElement: JavaElement): T? {
    return firstOrNull {
        member ->
        (member.getSource() as? JavaSourceElement)?.javaElement == javaElement
    }
}
