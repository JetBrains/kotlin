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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor

abstract class OverridingStrategy {
    abstract fun addFakeOverride(fakeOverride: CallableMemberDescriptor)

    abstract fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor)

    abstract fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor)

    open fun setOverriddenDescriptors(member: CallableMemberDescriptor, overridden: Collection<CallableMemberDescriptor>) {
        member.overriddenDescriptors = overridden
    }
}

abstract class NonReportingOverrideStrategy : OverridingStrategy() {
    override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
        conflict(fromSuper, fromCurrent)
    }

    override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        conflict(first, second)
    }

    protected abstract fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor)
}
