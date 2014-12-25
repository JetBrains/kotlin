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

package org.jetbrains.jet.lang.resolve.scopes

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.resolve.name.Name

public class InnerClassesScopeWrapper(override val workerScope: JetScope) : AbstractScopeAdapter() {
    override fun getClassifier(name: Name) = workerScope.getClassifier(name) as? ClassDescriptor

    override fun getDeclarationsByLabel(labelName: Name) = workerScope.getDeclarationsByLabel(labelName).filterIsInstance<ClassDescriptor>()

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): List<ClassDescriptor> {
        val restrictedFilter = kindFilter.restrictedToKindsOrNull(DescriptorKindFilter.CLASSIFIERS_MASK) ?: return listOf()
        return workerScope.getDescriptors(restrictedFilter, nameFilter).filterIsInstance<ClassDescriptor>()
    }

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()

    override fun toString() = "Classes from " + workerScope
}
