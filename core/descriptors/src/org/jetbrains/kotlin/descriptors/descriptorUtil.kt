/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.sure

fun ModuleDescriptor.resolveClassByFqName(fqName: FqName, lookupLocation: LookupLocation): ClassDescriptor? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent())
            .memberScope.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor)?.let { return it }

    return resolveClassByFqName(fqName.parent(), lookupLocation)
            ?.unsubstitutedInnerClassesScope
            ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor
}

fun ModuleDescriptor.findContinuationClassDescriptorOrNull(lookupLocation: LookupLocation) =
        resolveClassByFqName(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME, lookupLocation)

fun ModuleDescriptor.findContinuationClassDescriptor(lookupLocation: LookupLocation) =
        findContinuationClassDescriptorOrNull(lookupLocation).sure { "Continuation interface is not found" }
