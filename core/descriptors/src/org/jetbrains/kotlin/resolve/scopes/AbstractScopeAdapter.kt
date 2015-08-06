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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.Printer

/**
 * Introduces a simple wrapper for internal scope.
 */
public abstract class AbstractScopeAdapter : JetScope {
    protected abstract val workerScope: JetScope

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        return workerScope.getImplicitReceiversHierarchy()
    }

    override fun getFunctions(name: Name, location: UsageLocation): Collection<FunctionDescriptor> {
        return workerScope.getFunctions(name, location)
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        return workerScope.getPackage(name)
    }

    override fun getClassifier(name: Name, location: UsageLocation): ClassifierDescriptor? {
        return workerScope.getClassifier(name, location)
    }

    override fun getProperties(name: Name, location: UsageLocation): Collection<VariableDescriptor> {
        return workerScope.getProperties(name, location)
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>, name: Name, location: UsageLocation): Collection<PropertyDescriptor> {
        return workerScope.getSyntheticExtensionProperties(receiverTypes, name, location)
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name, location: UsageLocation): Collection<FunctionDescriptor> {
        return workerScope.getSyntheticExtensionFunctions(receiverTypes, name, location)
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor> {
        return workerScope.getSyntheticExtensionProperties(receiverTypes)
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>): Collection<FunctionDescriptor> {
        return workerScope.getSyntheticExtensionFunctions(receiverTypes)
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        return workerScope.getLocalVariable(name)
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        return workerScope.getContainingDeclaration()
    }

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> {
        return workerScope.getDeclarationsByLabel(labelName)
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return workerScope.getDescriptors(kindFilter, nameFilter)
    }

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> {
        return workerScope.getOwnDeclaredDescriptors()
    }

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.print("worker =")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
