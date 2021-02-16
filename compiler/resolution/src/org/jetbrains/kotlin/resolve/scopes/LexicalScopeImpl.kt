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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.utils.Printer

class LexicalScopeImpl @JvmOverloads constructor(
    parent: HierarchicalScope,
    override val ownerDescriptor: DeclarationDescriptor,
    override val isOwnerDescriptorAccessibleByLabel: Boolean,
    override val implicitReceivers: List<ReceiverParameterDescriptor>,
    override val kind: LexicalScopeKind,
    redeclarationChecker: LocalRedeclarationChecker = LocalRedeclarationChecker.DO_NOTHING,
    initialize: LexicalScopeImpl.InitializeHandler.() -> Unit = {}
) : LexicalScope, LexicalScopeStorage(parent, redeclarationChecker) {

    init {
        InitializeHandler().initialize()
    }

    override fun toString(): String = kind.toString()

    override fun printStructure(p: Printer) {
        p.println(
            this::class.java.simpleName,
            ": ",
            kind,
            "; for descriptor: ",
            ownerDescriptor.name,
            " with implicitReceiver: ",
            if (implicitReceivers.isEmpty()) "NONE" else implicitReceivers.joinToString { it.value.toString() },
            " {"
        )
        p.pushIndent()

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

    inner class InitializeHandler() {

        fun addVariableDescriptor(variableDescriptor: VariableDescriptor): Unit =
            this@LexicalScopeImpl.addVariableOrClassDescriptor(variableDescriptor)

        fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor): Unit =
            this@LexicalScopeImpl.addFunctionDescriptorInternal(functionDescriptor)

        fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor): Unit =
            this@LexicalScopeImpl.addVariableOrClassDescriptor(classifierDescriptor)

    }
}
