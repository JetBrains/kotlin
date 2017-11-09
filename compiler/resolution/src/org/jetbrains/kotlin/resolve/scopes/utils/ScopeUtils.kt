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

package org.jetbrains.kotlin.resolve.scopes.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.SmartList

val HierarchicalScope.parentsWithSelf: Sequence<HierarchicalScope>
    get() = generateSequence(this) { it.parent }

val HierarchicalScope.parents: Sequence<HierarchicalScope>
    get() = parentsWithSelf.drop(1)

/**
 * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
 */
fun LexicalScope.getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = collectFromMeAndParent {
    (it as? LexicalScope)?.implicitReceiver
}

fun LexicalScope.getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = collectAllFromMeAndParent {
    if (it is LexicalScope && it.isOwnerDescriptorAccessibleByLabel && it.ownerDescriptor.name == labelName) {
        listOf(it.ownerDescriptor)
    }
    else {
        listOf()
    }
}

// Result is guaranteed to be filtered by kind and name.
fun HierarchicalScope.collectDescriptorsFiltered(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = { true },
        changeNamesForAliased: Boolean = false
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    return collectAllFromMeAndParent {
        if (it is ImportingScope)
            it.getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)
        else
            it.getContributedDescriptors(kindFilter, nameFilter)
    }.filter { kindFilter.accepts(it) && nameFilter(it.name) }
}

@Deprecated("Use getContributedProperties instead") fun LexicalScope.findLocalVariable(name: Name): VariableDescriptor? {
    return findFirstFromMeAndParent {
        when {
            it is LexicalScopeWrapper -> it.delegate.findLocalVariable(name)

            it !is ImportingScope && it !is LexicalChainedScope -> it.getContributedVariables(name, NoLookupLocation.WHEN_GET_LOCAL_VARIABLE).singleOrNull() /* todo check this*/

            else -> null
        }
    }
}

fun HierarchicalScope.findClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
        = findFirstFromMeAndParent { it.getContributedClassifier(name, location) }

fun HierarchicalScope.findPackage(name: Name): PackageViewDescriptor?
        = findFirstFromImportingScopes { it.getContributedPackage(name) }

fun HierarchicalScope.collectVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor>
        = collectAllFromMeAndParent { it.getContributedVariables(name, location) }

fun HierarchicalScope.collectFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor>
        = collectAllFromMeAndParent { it.getContributedFunctions(name, location) }

fun HierarchicalScope.findVariable(name: Name, location: LookupLocation, predicate: (VariableDescriptor) -> Boolean = { true }): VariableDescriptor? {
    processForMeAndParent {
        it.getContributedVariables(name, location).firstOrNull(predicate)?.let { return it }
    }
    return null
}

fun HierarchicalScope.findFunction(name: Name, location: LookupLocation, predicate: (FunctionDescriptor) -> Boolean = { true }): FunctionDescriptor? {
    processForMeAndParent {
        it.getContributedFunctions(name, location).firstOrNull(predicate)?.let { return it }
    }
    return null
}

fun HierarchicalScope.takeSnapshot(): HierarchicalScope = if (this is LexicalWritableScope) takeSnapshot() else this

@JvmOverloads fun MemberScope.memberScopeAsImportingScope(parentScope: ImportingScope? = null): ImportingScope = MemberScopeToImportingScopeAdapter(parentScope, this)

private class MemberScopeToImportingScopeAdapter(override val parent: ImportingScope?, val memberScope: MemberScope) : ImportingScope {
    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean, changeNamesForAliased: Boolean)
            = memberScope.getContributedDescriptors(kindFilter, nameFilter)

    override fun getContributedClassifier(name: Name, location: LookupLocation) = memberScope.getContributedClassifier(name, location)

    override fun getContributedVariables(name: Name, location: LookupLocation) = memberScope.getContributedVariables(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation) = memberScope.getContributedFunctions(name, location)

    override fun equals(other: Any?) = other is MemberScopeToImportingScopeAdapter && other.memberScope == memberScope

    override fun hashCode() = memberScope.hashCode()

    override fun toString() = "${this::class.java.simpleName} for $memberScope"

    override fun computeImportedNames() = memberScope.computeAllNames()

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName)
        p.pushIndent()

        memberScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}

inline fun HierarchicalScope.processForMeAndParent(process: (HierarchicalScope) -> Unit) {
    var currentScope = this
    while (true) {
        process(currentScope)
        currentScope = currentScope.parent ?: break
    }
}

private inline fun <T: Any> HierarchicalScope.collectFromMeAndParent(
        collect: (HierarchicalScope) -> T?
): List<T> {
    var result: MutableList<T>? = null
    processForMeAndParent {
        val element = collect(it)
        if (element != null) {
            if (result == null) {
                result = SmartList()
            }
            result!!.add(element)
        }
    }
    return result ?: emptyList()
}

inline fun <T: Any> HierarchicalScope.collectAllFromMeAndParent(
        collect: (HierarchicalScope) -> Collection<T>
): Collection<T> {
    var result: Collection<T>? = null
    processForMeAndParent { result = result.concat(collect(it)) }
    return result ?: emptySet()
}

inline fun <T: Any> HierarchicalScope.findFirstFromMeAndParent(fetch: (HierarchicalScope) -> T?): T? {
    processForMeAndParent { fetch(it)?.let { return it } }
    return null
}

inline fun <T: Any> HierarchicalScope.collectAllFromImportingScopes(
        collect: (ImportingScope) -> Collection<T>
): Collection<T> {
    return collectAllFromMeAndParent { if (it is ImportingScope) collect(it) else emptyList() }
}

inline fun <T: Any> HierarchicalScope.findFirstFromImportingScopes(fetch: (ImportingScope) -> T?): T? {
    return findFirstFromMeAndParent { if (it is ImportingScope) fetch(it) else null }
}

fun LexicalScope.addImportingScopes(importScopes: List<ImportingScope>): LexicalScope {
    val lastLexicalScope = parentsWithSelf.last { it is LexicalScope }
    val firstImporting = lastLexicalScope.parent as ImportingScope
    val newFirstImporting = chainImportingScopes(importScopes, firstImporting)
    return replaceImportingScopes(newFirstImporting)
}

fun LexicalScope.addImportingScope(importScope: ImportingScope): LexicalScope
        = addImportingScopes(listOf(importScope))

fun ImportingScope.withParent(newParent: ImportingScope?): ImportingScope {
    return object: ImportingScope by this {
        override val parent: ImportingScope?
            get() = newParent
    }
}

fun LexicalScope.replaceImportingScopes(importingScopeChain: ImportingScope?): LexicalScope {
    val newImportingScopeChain = importingScopeChain ?: ImportingScope.Empty
    if (this is LexicalScopeWrapper) {
        return LexicalScopeWrapper(this.delegate, newImportingScopeChain)
    }
    return LexicalScopeWrapper(this, newImportingScopeChain)
}

fun LexicalScope.createScopeForDestructuring(newReceiver: ReceiverParameterDescriptor?): LexicalScope {
    return LexicalScopeImpl(
            parent, ownerDescriptor, isOwnerDescriptorAccessibleByLabel,
            newReceiver,
            LexicalScopeKind.FUNCTION_HEADER_FOR_DESTRUCTURING
    )
}

private class LexicalScopeWrapper(val delegate: LexicalScope, val newImportingScopeChain: ImportingScope): LexicalScope by delegate {
    init {
        assert(delegate !is LexicalScopeWrapper) {
            "Do not wrap again to avoid performance issues"
        }
    }

    override val parent: HierarchicalScope by lazy(LazyThreadSafetyMode.NONE) {
        assert(delegate !is ImportingScope)

        val parent = delegate.parent
        if (parent is LexicalScope) {
            parent.replaceImportingScopes(newImportingScopeChain)
        }
        else {
            newImportingScopeChain
        }
    }

    override fun toString() = kind.toString()
}

fun chainImportingScopes(scopes: List<ImportingScope>, tail: ImportingScope? = null): ImportingScope? {
    return scopes.asReversed()
            .fold(tail) { current, scope ->
                assert(scope.parent == null)
                scope.withParent(current)
            }
}

class ThrowingLexicalScope : LexicalScope {
    override val parent: HierarchicalScope
        get() = throw IllegalStateException()

    override val ownerDescriptor: DeclarationDescriptor
        get() = throw IllegalStateException()
    override val isOwnerDescriptorAccessibleByLabel: Boolean
        get() = throw IllegalStateException()
    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = throw IllegalStateException()
    override val kind: LexicalScopeKind
        get() = LexicalScopeKind.THROWING

    override fun printStructure(p: Printer) =
            throw IllegalStateException()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            throw IllegalStateException()

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> =
            throw IllegalStateException()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
            throw IllegalStateException()

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
            throw IllegalStateException()
}
