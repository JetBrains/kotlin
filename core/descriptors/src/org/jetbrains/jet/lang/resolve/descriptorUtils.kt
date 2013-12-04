/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

public fun ClassDescriptor.checkSuperTypeByFQName(qualifiedName: String, deep: Boolean): Boolean {
    fun checkDescriptor(descriptor: DeclarationDescriptor): Boolean {
        return qualifiedName == DescriptorUtils.getFqName(descriptor).asString()
    }

    if (deep && checkDescriptor(this)) return true

    for (superType in getTypeConstructor().getSupertypes()) {
        val superDescriptor = superType.getConstructor().getDeclarationDescriptor()
        if (superDescriptor is ClassDescriptor) {
            if (checkDescriptor(superDescriptor)) return true
            if (deep && superDescriptor.checkSuperTypeByFQName(qualifiedName, deep)) return true
        }
    }

    return false
}
