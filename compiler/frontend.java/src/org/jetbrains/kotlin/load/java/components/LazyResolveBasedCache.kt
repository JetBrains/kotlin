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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaFieldImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaMethodImpl
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

class LazyResolveBasedCache(resolveSession: ResolveSession) : AbstractJavaResolverCache(resolveSession) {

    override fun recordMethod(method: JavaMethod, descriptor: SimpleFunctionDescriptor) {
        BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, (method as? JavaMethodImpl)?.psi ?: return, descriptor)
    }

    override fun recordConstructor(element: JavaElement, descriptor: ConstructorDescriptor) {
        trace.record(CONSTRUCTOR, (element as? JavaElementImpl<*>)?.psi ?: return, descriptor)
    }

    override fun recordField(field: JavaField, descriptor: PropertyDescriptor) {
        trace.record(VARIABLE, (field as? JavaFieldImpl)?.psi ?: return, descriptor)
    }

    override fun recordClass(javaClass: JavaClass, descriptor: ClassDescriptor) {
        trace.record(CLASS, (javaClass as? JavaClassImpl)?.psi ?: return, descriptor)
    }

}
