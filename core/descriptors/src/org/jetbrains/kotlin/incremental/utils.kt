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
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

fun LookupTracker.record(from: LookupLocation, scopeOwner: DeclarationDescriptor, name: Name) {
    if (this == LookupTracker.DO_NOTHING || from is NoLookupLocation) return

    val location = from.location ?: return

    val scopeKind = getScopeKind(scopeOwner) ?:
                    throw AssertionError("Unexpected containing declaration type: ${scopeOwner.javaClass}")

    val position = if (requiresPosition) location.position else Position.NO_POSITION

    record(location.filePath, position, scopeOwner.fqNameUnsafe.asString(), scopeKind, name.asString())
}

fun getScopeKind(scopeOwner: DeclarationDescriptor) = when (scopeOwner) {
    is ClassifierDescriptor -> ScopeKind.CLASSIFIER
    is PackageFragmentDescriptor -> ScopeKind.PACKAGE
    else -> null
}