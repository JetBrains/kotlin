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

@file:JvmName("ScopeUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.FileScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LexicalChainedScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.getFileScope


public fun JetScope.getAllAccessibleVariables(name: Name): Collection<VariableDescriptor>
        = getVariablesFromImplicitReceivers(name) + getProperties(name, NoLookupLocation.FROM_IDE) + listOfNotNull(getLocalVariable(name))

public fun JetScope.getAllAccessibleFunctions(name: Name): Collection<FunctionDescriptor>
        = getImplicitReceiversWithInstance().flatMap { it.type.memberScope.getFunctions(name, NoLookupLocation.FROM_IDE) } +
          getFunctions(name, NoLookupLocation.FROM_IDE)

public fun JetScope.getVariablesFromImplicitReceivers(name: Name): Collection<VariableDescriptor> = getImplicitReceiversWithInstance().flatMap {
    it.type.memberScope.getProperties(name, NoLookupLocation.FROM_IDE)
}

public fun JetScope.getVariableFromImplicitReceivers(name: Name): VariableDescriptor? {
    getImplicitReceiversWithInstance().forEach {
        it.type.memberScope.getProperties(name, NoLookupLocation.FROM_IDE).singleOrNull()?.let { return it }
    }
    return null
}

public fun LexicalScope.addImportScope(importScope: JetScope): LexicalScope {
    val fileScope = getFileScope()
    val scopeWithAdditionImport =
            LexicalChainedScope(fileScope, fileScope.ownerDescriptor, false, null, "Scope with addition import", importScope)
    return LexicalScopeWrapper(this, scopeWithAdditionImport)
}

private class LexicalScopeWrapper(val delegate: LexicalScope, val fileScopeReplace: LexicalScope): LexicalScope by delegate {
    override val parent: LexicalScope? by lazy(LazyThreadSafetyMode.NONE) {
        if (delegate is FileScope) {
            fileScopeReplace
        }
        else {
            LexicalScopeWrapper(delegate.parent!!, fileScopeReplace)
        }
    }
}