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

package org.jetbrains.kotlin.resolve.scopes.utils

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer

public val LexicalScope.parentsWithSelf: Sequence<LexicalScope>
    get() = sequence(this) { it.parent }

public val LexicalScope.parents: Sequence<LexicalScope>
    get() = parentsWithSelf.drop(1)

/**
 * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
 */
public fun LexicalScope.getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
    // todo remove hack
    var jetScopeRefactoringHack: KtScope? = null
    val receivers = collectFromMeAndParent {
        if (it is MemberScopeToImportingScopeAdapter) {
            jetScopeRefactoringHack = it.memberScope
        }
        it.implicitReceiver
    }

    return if (jetScopeRefactoringHack != null) {
        receivers + jetScopeRefactoringHack!!.getImplicitReceiversHierarchy()
    }
    else {
        receivers
    }
}

public fun LexicalScope.getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = collectAllFromMeAndParent {
    if(it is MemberScopeToImportingScopeAdapter) { // todo remove this hack
        it.memberScope.getDeclarationsByLabel(labelName)
    }
    else if (it.isOwnerDescriptorAccessibleByLabel && it.ownerDescriptor.name == labelName) {
        listOf(it.ownerDescriptor)
    }
    else {
        listOf()
    }
}

// Result is guaranteed to be filtered by kind and name.
public fun LexicalScope.collectDescriptorsFiltered(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = { true }
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    return collectAllFromMeAndParent { it.getContributedDescriptors(kindFilter, nameFilter) }
            .filter { kindFilter.accepts(it) && nameFilter(it.name) }
}


@Deprecated("Use getOwnProperties instead")
public fun LexicalScope.findLocalVariable(name: Name): VariableDescriptor? {
    return findFirstFromMeAndParent {
        when {
            it is LexicalScopeWrapper -> it.delegate.findLocalVariable(name)

            it is MemberScopeToImportingScopeAdapter -> it.memberScope.getLocalVariable(name) /* todo remove hack*/

            it !is ImportingScope && it !is LexicalChainedScope -> it.getContributedVariables(name, NoLookupLocation.UNSORTED).singleOrNull() /* todo check this*/

            else -> null
        }
    }
}

public fun LexicalScope.findClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
        = findFirstFromMeAndParent { it.getContributedClassifier(name, location) }

public fun LexicalScope.findPackage(name: Name): PackageViewDescriptor?
        = findFirstFromImportingScopes { it.getContributedPackage(name) }

public fun LexicalScope.collectVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor>
        = collectAllFromMeAndParent { it.getContributedVariables(name, location) }

public fun LexicalScope.collectFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor>
        = collectAllFromMeAndParent { it.getContributedFunctions(name, location) }

public fun LexicalScope.findVariable(name: Name, location: LookupLocation, predicate: (VariableDescriptor) -> Boolean = { true }): VariableDescriptor? {
    processForMeAndParent {
        it.getContributedVariables(name, location).firstOrNull(predicate)?.let { return it }
    }
    return null
}

public fun LexicalScope.findFunction(name: Name, location: LookupLocation, predicate: (FunctionDescriptor) -> Boolean = { true }): FunctionDescriptor? {
    processForMeAndParent {
        it.getContributedFunctions(name, location).firstOrNull(predicate)?.let { return it }
    }
    return null
}

public fun LexicalScope.collectSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
        = collectAllFromImportingScopes { it.getContributedSyntheticExtensionProperties(receiverTypes, name, location) }

public fun LexicalScope.collectSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
        = collectAllFromImportingScopes { it.getContributedSyntheticExtensionFunctions(receiverTypes, name, location) }

public fun LexicalScope.collectSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>)
        = collectAllFromImportingScopes { it.getContributedSyntheticExtensionProperties(receiverTypes) }

public fun LexicalScope.collectSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>)
        = collectAllFromImportingScopes { it.getContributedSyntheticExtensionFunctions(receiverTypes) }

public fun LexicalScope.takeSnapshot(): LexicalScope = if (this is LexicalWritableScope) takeSnapshot() else this

public fun LexicalScope.asKtScope(): KtScope {
    if (this is KtScope) return this
    if (this is MemberScopeToImportingScopeAdapter) return this.memberScope
    return LexicalToKtScopeAdapter(this)
}

@JvmOverloads
public fun KtScope.memberScopeAsImportingScope(parentScope: ImportingScope? = null): ImportingScope = MemberScopeToImportingScopeAdapter(parentScope, this)

private class LexicalToKtScopeAdapter(lexicalScope: LexicalScope): KtScope {
    val lexicalScope = lexicalScope.takeSnapshot()

    override fun getClassifier(name: Name, location: LookupLocation) = lexicalScope.findClassifier(name, location)

    override fun getPackage(name: Name) = lexicalScope.findPackage(name)

    override fun getProperties(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return lexicalScope.collectAllFromImportingScopes { it.getContributedVariables(name, location) }
    }

    override fun getFunctions(name: Name, location: LookupLocation)
            = lexicalScope.collectFunctions(name, location)

    override fun getLocalVariable(name: Name) = lexicalScope.findLocalVariable(name)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = lexicalScope.collectSyntheticExtensionProperties(receiverTypes, name, location)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = lexicalScope.collectSyntheticExtensionFunctions(receiverTypes, name, location)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>)
            = lexicalScope.collectSyntheticExtensionProperties(receiverTypes)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>)
            = lexicalScope.collectSyntheticExtensionFunctions(receiverTypes)

    override fun getContainingDeclaration() = lexicalScope.ownerDescriptor

    override fun getDeclarationsByLabel(labelName: Name) = lexicalScope.getDeclarationsByLabel(labelName)

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return lexicalScope.collectAllFromMeAndParent { it.getContributedDescriptors(kindFilter, nameFilter) }
    }

    override fun getImplicitReceiversHierarchy() = lexicalScope.getImplicitReceiversHierarchy()
    override fun getOwnDeclaredDescriptors() = lexicalScope.getContributedDescriptors()

    override fun equals(other: Any?) = other is LexicalToKtScopeAdapter && other.lexicalScope == this.lexicalScope

    override fun hashCode() = lexicalScope.hashCode()

    override fun toString() = "LexicalToKtScopeAdapter for $lexicalScope"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName)
        p.pushIndent()

        lexicalScope.printStructure(p)

        p.popIndent()
        p.println("}")
    }
}

private class MemberScopeToImportingScopeAdapter(override val parent: ImportingScope?, val memberScope: KtScope) : ImportingScope {
    override fun getContributedPackage(name: Name): PackageViewDescriptor? = memberScope.getPackage(name)

    override fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = memberScope.getSyntheticExtensionProperties(receiverTypes, name, location)

    override fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = memberScope.getSyntheticExtensionFunctions(receiverTypes, name, location)

    override fun getContributedSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>)
            = memberScope.getSyntheticExtensionProperties(receiverTypes)

    override fun getContributedSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>)
            = memberScope.getSyntheticExtensionFunctions(receiverTypes)

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = memberScope.getDescriptors(kindFilter, nameFilter)

    override val ownerDescriptor: DeclarationDescriptor
        get() = memberScope.getContainingDeclaration()

    override fun getContributedClassifier(name: Name, location: LookupLocation) = memberScope.getClassifier(name, location)

    override fun getContributedVariables(name: Name, location: LookupLocation) = memberScope.getProperties(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation) = memberScope.getFunctions(name, location)

    override fun equals(other: Any?) = other is MemberScopeToImportingScopeAdapter && other.memberScope == memberScope

    override fun hashCode() = memberScope.hashCode()

    override fun toString() = "${javaClass.simpleName} for $memberScope"

    override fun printStructure(p: Printer) {
        p.println(javaClass.simpleName)
        p.pushIndent()

        memberScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}

inline fun LexicalScope.processForMeAndParent(process: (LexicalScope) -> Unit) {
    var currentScope = this
    while (true) {
        process(currentScope)
        currentScope = currentScope.parent ?: break
    }
}

private inline fun <T: Any> LexicalScope.collectFromMeAndParent(
        collect: (LexicalScope) -> T?
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

inline fun <T: Any> LexicalScope.collectAllFromMeAndParent(
        collect: (LexicalScope) -> Collection<T>
): Collection<T> {
    var result: Collection<T>? = null
    processForMeAndParent { result = result.concat(collect(it)) }
    return result ?: emptySet()
}

inline fun <T: Any> LexicalScope.findFirstFromMeAndParent(fetch: (LexicalScope) -> T?): T? {
    processForMeAndParent { fetch(it)?.let { return it } }
    return null
}

inline fun <T: Any> LexicalScope.collectAllFromImportingScopes(
        collect: (ImportingScope) -> Collection<T>
): Collection<T> {
    return collectAllFromMeAndParent { if (it is ImportingScope) collect(it) else emptyList() }
}

inline fun <T: Any> LexicalScope.findFirstFromImportingScopes(fetch: (ImportingScope) -> T?): T? {
    return findFirstFromMeAndParent { if (it is ImportingScope) fetch(it) else null }
}

fun LexicalScope.addImportingScopes(importScopes: List<ImportingScope>): LexicalScope {
    if (this is ImportingScope) {
        return chainImportingScopes(importScopes, this)!!
    }
    else {
        val lastNonImporting = parentsWithSelf.last { it !is ImportingScope }
        val firstImporting = lastNonImporting.parent as ImportingScope?
        val newFirstImporting = chainImportingScopes(importScopes, firstImporting)
        return LexicalScopeWrapper(this, newFirstImporting)
    }
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
    return if (this is ImportingScope)
        importingScopeChain ?: ImportingScope.Empty
    else
        LexicalScopeWrapper(this, importingScopeChain)
}

private class LexicalScopeWrapper(val delegate: LexicalScope, val newImportingScopeChain: ImportingScope?): LexicalScope by delegate {
    override val parent: LexicalScope? by lazy(LazyThreadSafetyMode.NONE) {
        assert(delegate !is ImportingScope)

        val parent = delegate.parent
        if (parent == null || parent is ImportingScope) {
            newImportingScopeChain
        }
        else {
            LexicalScopeWrapper(parent, newImportingScopeChain)
        }
    }
}

fun chainImportingScopes(scopes: List<ImportingScope>, tail: ImportingScope? = null): ImportingScope? {
    return scopes.asReversed()
            .fold(tail) { current, scope ->
                assert(scope.parent == null)
                scope.withParent(current)
            }
}
