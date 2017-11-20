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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer

class InnerClassesScopeWrapper(val workerScope: MemberScope) : MemberScopeImpl() {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            workerScope.getContributedClassifier(name, location)?.let {
                it as? ClassDescriptor ?: it as? TypeAliasDescriptor
            }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<ClassifierDescriptor> {
        val restrictedFilter = kindFilter.restrictedToKindsOrNull(DescriptorKindFilter.CLASSIFIERS_MASK) ?: return listOf()
        return workerScope.getContributedDescriptors(restrictedFilter, nameFilter).filterIsInstance<ClassifierDescriptorWithTypeParameters>()
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InnerClassesScopeWrapper for scope:")
        workerScope.printScopeStructure(p)
    }

    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()

    override fun definitelyDoesNotContainName(name: Name) = workerScope.definitelyDoesNotContainName(name)

    override fun toString() = "Classes from $workerScope"
}
