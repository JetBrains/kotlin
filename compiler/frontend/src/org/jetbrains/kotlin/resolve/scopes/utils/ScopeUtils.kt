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
import org.jetbrains.kotlin.resolve.lazy.LazyImportingScope
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer

public fun LexicalScope.getFileScope(): ImportingScope {
    var currentScope = this
    while(currentScope.parent != null) {
        currentScope = currentScope.parent!!
    }
    assert(currentScope is ImportingScope) {
        "Not FileScope without parent: $currentScope" // todo improve debug message
    }
    return currentScope as ImportingScope
}

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
public fun LexicalScope.getDescriptorsFiltered(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = { true }
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    return collectAllFromMeAndParent {
        if (it is ImportingScope) {
            it.getDescriptors(kindFilter, nameFilter)
        } else {
            it.getDeclaredDescriptors()
        }
    }.filter { kindFilter.accepts(it) && nameFilter(it.name) }
}


@Deprecated("Use getOwnProperties instead")
public fun LexicalScope.getLocalVariable(name: Name): VariableDescriptor? {
    processForMeAndParent {
        if (it is LexicalScopeWrapper) {
            return it.delegate.getLocalVariable(name)
        }
        else if (it is LazyImportingScope) {
            return it.getLocalVariable(name) // todo: remove hack for repl interpreter
        }
        else if (it is MemberScopeToImportingScopeAdapter) { // todo remove hack
            return it.memberScope.getLocalVariable(name)
        }
        else if (it !is ImportingScope && it !is LexicalChainedScope) { // todo check this
            it.getDeclaredVariables(name, NoLookupLocation.UNSORTED).singleOrNull()?.let { return it }
        }
    }
    return null
}

public fun LexicalScope.getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
    processForMeAndParent {
        it.getDeclaredClassifier(name, location)?.let { return it }
    }
    return null
}

public fun LexicalScope.takeSnapshot(): LexicalScope = if (this is LexicalWritableScope) takeSnapshot() else this

public fun LexicalScope.asJetScope(): KtScope {
    if (this is KtScope) return this
    if (this is MemberScopeToImportingScopeAdapter) return this.memberScope
    return LexicalToJetScopeAdapter(this)
}

public fun KtScope.memberScopeAsFileScope(): ImportingScope = MemberScopeToImportingScopeAdapter(this)

@Deprecated("Remove this method after scope refactoring")
public fun KtScope.asLexicalScope(): LexicalScope
        = if (this is LexicalToJetScopeAdapter) {
            lexicalScope
        }
        else {
            memberScopeAsFileScope()
        }

private class LexicalToJetScopeAdapter(lexicalScope: LexicalScope): KtScope {
    val lexicalScope = lexicalScope.takeSnapshot()

    override fun getClassifier(name: Name, location: LookupLocation) = lexicalScope.getClassifier(name, location)

    override fun getPackage(name: Name) = lexicalScope.getFileScope().getPackage(name)

    override fun getProperties(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        val fileScope = lexicalScope.getFileScope()
        if (fileScope is MemberScopeToImportingScopeAdapter) {
            return fileScope.memberScope.getProperties(name, location)
        }
        else {
            return fileScope.getDeclaredVariables(name, location)
        }
    }

    override fun getLocalVariable(name: Name) = lexicalScope.getLocalVariable(name)

    override fun getFunctions(name: Name, location: LookupLocation) = lexicalScope.collectAllFromMeAndParent {
        it.getDeclaredFunctions(name, location)
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = lexicalScope.getFileScope().getSyntheticExtensionProperties(receiverTypes, name, location)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = lexicalScope.getFileScope().getSyntheticExtensionFunctions(receiverTypes, name, location)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>)
        = lexicalScope.getFileScope().getSyntheticExtensionProperties(receiverTypes)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>)
        = lexicalScope.getFileScope().getSyntheticExtensionFunctions(receiverTypes)

    override fun getContainingDeclaration() = lexicalScope.ownerDescriptor

    override fun getDeclarationsByLabel(labelName: Name) = lexicalScope.getDeclarationsByLabel(labelName)

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = lexicalScope.collectAllFromMeAndParent {
        if (it is ImportingScope) {
            it.getDescriptors(kindFilter, nameFilter)
        } else it.getDeclaredDescriptors()
    }

    override fun getImplicitReceiversHierarchy() = lexicalScope.getImplicitReceiversHierarchy()
    override fun getOwnDeclaredDescriptors() = lexicalScope.getDeclaredDescriptors()

    override fun equals(other: Any?) = other is LexicalToJetScopeAdapter && other.lexicalScope == this.lexicalScope

    override fun hashCode() = lexicalScope.hashCode()

    override fun toString() = "LexicalToJetScopeAdapter for $lexicalScope"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName)
        p.pushIndent()

        lexicalScope.printStructure(p)

        p.popIndent()
        p.println("}")
    }
}

private class MemberScopeToImportingScopeAdapter(val memberScope: KtScope) : ImportingScope {
    override val parent: ImportingScope?
        get() = null

    override fun getPackage(name: Name): PackageViewDescriptor? = memberScope.getPackage(name)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = memberScope.getSyntheticExtensionProperties(receiverTypes, name, location)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation)
            = memberScope.getSyntheticExtensionFunctions(receiverTypes, name, location)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>)
            = memberScope.getSyntheticExtensionProperties(receiverTypes)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>)
            = memberScope.getSyntheticExtensionFunctions(receiverTypes)

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = memberScope.getDescriptors(kindFilter, nameFilter)

    override val ownerDescriptor: DeclarationDescriptor
        get() = memberScope.getContainingDeclaration()

    override fun getDeclaredDescriptors() = memberScope.getOwnDeclaredDescriptors()

    override fun getDeclaredClassifier(name: Name, location: LookupLocation) = memberScope.getClassifier(name, location)

    override fun getDeclaredVariables(name: Name, location: LookupLocation) = memberScope.getProperties(name, location)

    override fun getDeclaredFunctions(name: Name, location: LookupLocation) = memberScope.getFunctions(name, location)

    override fun equals(other: Any?) = other is MemberScopeToImportingScopeAdapter && other.memberScope == memberScope

    override fun hashCode() = memberScope.hashCode()

    override fun toString() = "MemberScopeToFileScopeAdapter for $memberScope"

    override fun printStructure(p: Printer) {
        p.println(javaClass.simpleName)
        p.pushIndent()

        memberScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}

private inline fun LexicalScope.processForMeAndParent(process: (LexicalScope) -> Unit) {
    var currentScope = this
    process(currentScope)

    while(currentScope.parent != null) {
        currentScope = currentScope.parent!!
        process(currentScope)
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

internal inline fun <T: Any> LexicalScope.collectAllFromMeAndParent(
        collect: (LexicalScope) -> Collection<T>
): Collection<T> {
    var result: Collection<T>? = null
    processForMeAndParent { result = result.concat(collect(it)) }
    return result ?: emptySet()
}

public fun LexicalScope.addImportScope(importScope: KtScope): LexicalScope {
    val fileScope = getFileScope()
    val scopeWithAdditionImport =
            LexicalChainedScope(fileScope, fileScope.ownerDescriptor, false, null, "Scope with addition import", importScope)
    return replaceFileScope(scopeWithAdditionImport)
}

public fun LexicalScope.replaceFileScope(fileScopeReplace: LexicalScope): LexicalScope {
    if (this is ImportingScope) return fileScopeReplace

    return LexicalScopeWrapper(this, fileScopeReplace)
}

public fun LexicalScope.withNoFileScope(): LexicalScope = replaceFileScope(MemberScopeToImportingScopeAdapter(KtScope.Empty))

private class LexicalScopeWrapper(val delegate: LexicalScope, val fileScopeReplace: LexicalScope): LexicalScope by delegate {
    override val parent: LexicalScope? by lazy(LazyThreadSafetyMode.NONE) {
        assert(delegate !is ImportingScope) { "We should replace FileScope($delegate) to $fileScopeReplace" }
        val parent = delegate.parent!!

        if (parent is ImportingScope) {
            fileScopeReplace
        }
        else {
            LexicalScopeWrapper(parent, fileScopeReplace)
        }
    }
}

