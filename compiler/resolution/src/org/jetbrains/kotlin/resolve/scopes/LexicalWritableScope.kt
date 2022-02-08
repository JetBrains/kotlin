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
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class LexicalWritableScope(
    parent: LexicalScope,
    override val ownerDescriptor: DeclarationDescriptor,
    override val isOwnerDescriptorAccessibleByLabel: Boolean,
    redeclarationChecker: LocalRedeclarationChecker,
    override val kind: LexicalScopeKind
) : LexicalScopeStorage(parent, redeclarationChecker) {

    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = null
    override val contextReceiversGroup: List<ReceiverParameterDescriptor>
        get() = emptyList()

    private var canWrite: Boolean = true
    private var lastSnapshot: Snapshot? = null

    fun freeze() {
        canWrite = false
    }

    fun takeSnapshot(): LexicalScope {
        if (lastSnapshot == null || lastSnapshot!!.descriptorLimit != addedDescriptors.size) {
            lastSnapshot = Snapshot(addedDescriptors.size)
        }
        return lastSnapshot!!
    }

    fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(variableDescriptor)
    }

    fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()
        addFunctionDescriptorInternal(functionDescriptor)
    }

    fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(classifierDescriptor)
    }

    private fun checkMayWrite() {
        if (!canWrite) {
            throw IllegalStateException("Cannot write into freezed scope:" + toString())
        }
    }

    private inner class Snapshot(val descriptorLimit: Int) : LexicalScope by this {
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
            addedDescriptors.subList(0, descriptorLimit)

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor

        // NB. This is important to have this explicit override, otherwise calls will be delegated to `this`-delegate,
        // which will use default implementation from `ResolutionScope`, which will call `getContributedClassifier` on
        // the `LexicalWritableScope` instead of calling it on this snapshot
        override fun getContributedClassifierIncludeDeprecated(
            name: Name,
            location: LookupLocation
        ): DescriptorWithDeprecation<ClassifierDescriptor>? {
            return variableOrClassDescriptorByName(name, descriptorLimit)
                ?.safeAs<ClassifierDescriptor>()
                ?.let { DescriptorWithDeprecation.createNonDeprecated(it) }
        }

        override fun getContributedVariables(name: Name, location: LookupLocation) =
            listOfNotNull(variableOrClassDescriptorByName(name, descriptorLimit) as? VariableDescriptor)

        override fun getContributedFunctions(name: Name, location: LookupLocation) = functionsByName(name, descriptorLimit)


        override fun toString(): String = "Snapshot($descriptorLimit) for $kind"

        override fun printStructure(p: Printer) {
            p.println("Snapshot with descriptorLimit = $descriptorLimit for scope:")
            this@LexicalWritableScope.printStructure(p)
        }
    }

    override fun toString(): String = kind.toString()

    override fun printStructure(p: Printer) {
        p.println(
            this::class.java.simpleName,
            ": ",
            kind,
            "; for descriptor: ",
            ownerDescriptor.name,
            " with implicitReceivers: ",
            implicitReceiver?.value ?: "NONE",
            " with contextReceiversGroup: ",
            if (contextReceiversGroup.isEmpty()) "NONE" else contextReceiversGroup.joinToString { it.value.toString() },
            " {"
        )
        p.pushIndent()

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
