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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.util.LinkedHashSet
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import java.util.HashSet

public fun JetScope.getImplicitReceiversWithInstance(): List<ReceiverParameterDescriptor> {
    // we use a set to workaround a bug with receiver for default object present twice in the result of getImplicitReceiversHierarchy()
    val receivers = LinkedHashSet(getImplicitReceiversHierarchy())

    val withInstance = HashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = getContainingDeclaration()
    while (current != null) {
        withInstance.add(current)
        val classDescriptor = current as? ClassDescriptor
        if (classDescriptor != null && !classDescriptor.isInner()) break
        current = current!!.getContainingDeclaration()
    }

    return receivers.filter {
        val owner = it.getContainingDeclaration()
        owner is ClassDescriptor && owner.getKind().isSingleton() || owner in withInstance
    }
}
