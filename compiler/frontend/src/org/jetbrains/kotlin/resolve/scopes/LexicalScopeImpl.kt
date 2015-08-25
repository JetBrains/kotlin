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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer

public class LexicalScopeImpl jvmOverloads constructor(
        override val parent: LexicalScope,
        override val ownerDescriptor: DeclarationDescriptor,
        override val isOwnerDescriptorAccessibleByLabel: Boolean,
        override val implicitReceiver: ReceiverParameterDescriptor?,
        private val debugName: String,
        redeclarationHandler: RedeclarationHandler = RedeclarationHandler.DO_NOTHING,
        initialize: LexicalScopeImpl.InitializeHandler.() -> Unit = {}
): LexicalScope, WritableScopeStorage {
    override val addedDescriptors: MutableList<DeclarationDescriptor> = SmartList()
    override val redeclarationHandler: RedeclarationHandler
        get() = RedeclarationHandler.DO_NOTHING

    override var functionsByName: MutableMap<Name, WritableScopeStorage.IntList>? = null
    override var variablesAndClassifiersByName: MutableMap<Name, WritableScopeStorage.IntList>? = null

    init {
        InitializeHandler(redeclarationHandler).initialize()
    }

    override fun getDeclaredDescriptors() = addedDescriptors

    override fun getDeclaredClassifier(name: Name, location: LookupLocation) = getDeclaredClassifier(name)

    override fun getDeclaredVariables(name: Name, location: LookupLocation) = getDeclaredVariables(name)

    override fun getDeclaredFunctions(name: Name, location: LookupLocation) = getDeclaredFunctions(name)

    override fun toString(): String = debugName

    override fun printStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugName, "; for descriptor: ", ownerDescriptor.name,
                  " with implicitReceiver: ", implicitReceiver?.value, " {")
        p.pushIndent()

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

    inner class InitializeHandler(override val redeclarationHandler: RedeclarationHandler): WritableScopeStorage {
        override val addedDescriptors: MutableList<DeclarationDescriptor>
            get() = this@LexicalScopeImpl.addedDescriptors
        override var functionsByName: MutableMap<Name, WritableScopeStorage.IntList>?
            get() = this@LexicalScopeImpl.functionsByName
            set(value) {
                this@LexicalScopeImpl.functionsByName = value
            }
        override var variablesAndClassifiersByName: MutableMap<Name, WritableScopeStorage.IntList>?
            get() = this@LexicalScopeImpl.variablesAndClassifiersByName
            set(value) {
                this@LexicalScopeImpl.variablesAndClassifiersByName = value
            }

        public fun addVariableDescriptor(variableDescriptor: VariableDescriptor): Unit
                = addVariableOrClassDescriptor(variableDescriptor)

        public override fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor): Unit
                = super.addFunctionDescriptor(functionDescriptor)

        public fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor): Unit
                = addVariableOrClassDescriptor(classifierDescriptor)

    }
}
