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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.scopes.GivenFunctionsMemberScope
import org.jetbrains.kotlin.storage.StorageManager

class FunctionClassScope(
        storageManager: StorageManager,
        containingClass: FunctionClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> =
            when ((containingClass as FunctionClassDescriptor).functionKind) {
                FunctionClassDescriptor.Kind.Function -> listOf(FunctionInvokeDescriptor.create(containingClass, isSuspend = false))
                FunctionClassDescriptor.Kind.SuspendFunction -> listOf(FunctionInvokeDescriptor.create(containingClass, isSuspend = true))
                else -> emptyList()
            }
}
