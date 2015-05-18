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

import com.google.common.collect.Multimap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name

public trait WritableScope : JetScope {
    public enum class LockLevel {
        WRITING,
        BOTH,
        READING
    }

    public fun changeLockLevel(lockLevel: LockLevel): WritableScope

    public fun addLabeledDeclaration(descriptor: DeclarationDescriptor)

    public fun addVariableDescriptor(variableDescriptor: VariableDescriptor)

    public fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor)

    public fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor)

    public fun importScope(imported: JetScope)

    public fun setImplicitReceiver(implicitReceiver: ReceiverParameterDescriptor)
}

