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

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer

import java.util.*

public abstract class WritableScopeWithImports(override val workerScope: JetScope,
                                               protected val redeclarationHandler: RedeclarationHandler,
                                               private val debugName: String) : AbstractScopeAdapter(), WritableScope {

    private var imports: MutableList<JetScope>? = null
    private var currentIndividualImportScope: WritableScope? = null
    private var implicitReceiverHierarchy: List<ReceiverParameterDescriptor>? = null


    private var lockLevel: WritableScope.LockLevel = WritableScope.LockLevel.WRITING

    override fun changeLockLevel(lockLevel: WritableScope.LockLevel): WritableScope {
        if (lockLevel.ordinal() < this.lockLevel.ordinal()) {
            throw IllegalStateException("cannot lower lock level from " + this.lockLevel + " to " + lockLevel + " at " + toString())
        }
        this.lockLevel = lockLevel
        return this
    }

    protected fun checkMayRead() {
        if (lockLevel != WritableScope.LockLevel.READING && lockLevel != WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot read with lock level " + lockLevel + " at " + toString())
        }
    }

    protected fun checkMayWrite() {
        if (lockLevel != WritableScope.LockLevel.WRITING && lockLevel != WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot write with lock level " + lockLevel + " at " + toString())
        }
    }

    protected fun checkMayNotWrite() {
        if (lockLevel == WritableScope.LockLevel.WRITING || lockLevel == WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot write with lock level " + lockLevel + " at " + toString())
        }
    }

    protected fun getImports(): MutableList<JetScope> {
        if (imports == null) {
            imports = ArrayList<JetScope>()
        }
        return imports!!
    }

    override fun importScope(imported: JetScope) {
        if (imported == this) {
            throw IllegalStateException("cannot import scope into self")
        }

        checkMayWrite()

        getImports().add(0, imported)
        currentIndividualImportScope = null
    }

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        checkMayRead()

        if (implicitReceiverHierarchy == null) {
            implicitReceiverHierarchy = computeImplicitReceiversHierarchy()
        }
        return implicitReceiverHierarchy!!
    }

    protected open fun computeImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        val implicitReceiverHierarchy = Lists.newArrayList<ReceiverParameterDescriptor>()
        // Imported scopes come with their receivers
        // Example: class member resolution scope imports a scope of it's default object
        //          members of the default object must be able to find it as an implicit receiver
        for (scope in getImports()) {
            implicitReceiverHierarchy.addAll(scope.getImplicitReceiversHierarchy())
        }
        implicitReceiverHierarchy.addAll(super<AbstractScopeAdapter>.getImplicitReceiversHierarchy())
        return implicitReceiverHierarchy
    }

    override fun getProperties(name: Name): Set<VariableDescriptor> {
        checkMayRead()

        val properties = Sets.newLinkedHashSet<VariableDescriptor>()
        for (imported in getImports()) {
            properties.addAll(imported.getProperties(name))
        }
        return properties
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        checkMayRead()

        // Meaningful lookup goes here
        for (imported in getImports()) {
            val importedDescriptor = imported.getLocalVariable(name)
            if (importedDescriptor != null) {
                return importedDescriptor
            }
        }
        return null
    }

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
        checkMayRead()

        if (getImports().isEmpty()) {
            return setOf()
        }
        val result = Sets.newLinkedHashSet<FunctionDescriptor>()
        for (imported in getImports()) {
            result.addAll(imported.getFunctions(name))
        }
        return result
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? {
        checkMayRead()

        for (imported in getImports()) {
            val importedClassifier = imported.getClassifier(name)
            if (importedClassifier != null) {
                return importedClassifier
            }
        }
        return null
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        checkMayRead()

        for (imported in getImports()) {
            val importedDescriptor = imported.getPackage(name)
            if (importedDescriptor != null) {
                return importedDescriptor
            }
        }
        return null
    }

    private fun getCurrentIndividualImportScope(): WritableScope {
        if (currentIndividualImportScope == null) {
            val writableScope = WritableScopeImpl(JetScope.Empty, getContainingDeclaration(), RedeclarationHandler.DO_NOTHING, "Individual import scope")
            writableScope.changeLockLevel(WritableScope.LockLevel.BOTH)
            importScope(writableScope)
            currentIndividualImportScope = writableScope
        }
        return currentIndividualImportScope!!
    }

    override fun importClassifierAlias(importedClassifierName: Name, classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        getCurrentIndividualImportScope().addClassifierAlias(importedClassifierName, classifierDescriptor)
    }

    override fun importPackageAlias(aliasName: Name, packageView: PackageViewDescriptor) {
        checkMayWrite()

        getCurrentIndividualImportScope().addPackageAlias(aliasName, packageView)
    }

    override fun importFunctionAlias(aliasName: Name, functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        getCurrentIndividualImportScope().addFunctionAlias(aliasName, functionDescriptor)
    }

    override fun importVariableAlias(aliasName: Name, variableDescriptor: VariableDescriptor) {
        checkMayWrite()

        getCurrentIndividualImportScope().addVariableAlias(aliasName, variableDescriptor)
    }

    override fun clearImports() {
        currentIndividualImportScope = null
        getImports().clear()
    }

    override fun toString(): String {
        return javaClass.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + " " + debugName + " for " + getContainingDeclaration()
    }

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " for ", getContainingDeclaration(), " {")
        p.pushIndent()

        p.println("lockLevel = ", lockLevel)

        printAdditionalScopeStructure(p)

        p.print("worker = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        if (getImports().isEmpty()) {
            p.println("imports = {}")
        }
        else {
            p.println("imports = {")
            p.pushIndent()
            for (anImport in getImports()) {
                anImport.printScopeStructure(p)
            }
            p.popIndent()
            p.println("}")
        }

        p.popIndent()
        p.println("}")
    }

    protected abstract fun printAdditionalScopeStructure(p: Printer)
}
