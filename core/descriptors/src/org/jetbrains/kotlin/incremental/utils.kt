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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope

public fun LookupTracker.record(from: LookupLocation, scopeOwner: DeclarationDescriptor, inScope: MemberScope, name: Name) {
    if (this == LookupTracker.DO_NOTHING || from is NoLookupLocation) return

    val location = from.location ?: return

    val scopeKind =
            when (scopeOwner) {
                is ClassifierDescriptor -> ScopeKind.CLASSIFIER
                is PackageFragmentDescriptor -> ScopeKind.PACKAGE
                else -> throw AssertionError("Unexpected containing declaration type: ${scopeOwner.javaClass}")
            }

    record(location, scopeOwner.fqNameUnsafe.asString(), scopeKind, name.asString())
}
