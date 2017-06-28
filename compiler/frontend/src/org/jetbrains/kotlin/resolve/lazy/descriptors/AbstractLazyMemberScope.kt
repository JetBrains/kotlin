/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import com.google.common.collect.Sets
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.AbstractPsiBasedDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import java.util.*

abstract class AbstractLazyMemberScope<out D : DeclarationDescriptor, out DP : DeclarationProvider>
protected constructor(
        protected val c: LazyClassContext,
        protected val declarationProvider: DP,
        protected val thisDescriptor: D,
        protected val trace: BindingTrace
) : MemberScopeImpl() {

    protected val storageManager: StorageManager = c.storageManager
    private val classDescriptors: MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> = storageManager.createMemoizedFunction { doGetClasses(it) }
    private val functionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> = storageManager.createMemoizedFunction { doGetFunctions(it) }
    private val propertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> = storageManager.createMemoizedFunction { doGetProperties(it) }
    private val typeAliasDescriptors: MemoizedFunctionToNotNull<Name, Collection<TypeAliasDescriptor>> = storageManager.createMemoizedFunction { doGetTypeAliases(it) }

    private fun doGetClasses(name: Name): List<ClassDescriptor> {
        val result = Sets.newLinkedHashSet<ClassDescriptor>()
        declarationProvider.getClassOrObjectDeclarations(name).mapTo(result) {
            if (it is KtScriptInfo)
                LazyScriptDescriptor(c as ResolveSession, thisDescriptor, name, it)
            else {
                val isExternal = it.modifierList?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: false
                LazyClassDescriptor(c, thisDescriptor, name, it, isExternal)
            }
        }
        getNonDeclaredClasses(name, result)
        return result.toList()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)
        // NB we should resolve type alias descriptors even if a class descriptor with corresponding name is present
        val classes = classDescriptors(name)
        val typeAliases = typeAliasDescriptors(name)
        var resultingClass: ClassDescriptor? = null
        for (klass in classes) {
            // See getFirstClassifierDiscriminateHeaders()
            if (!klass.isHeader) return klass
            if (resultingClass == null) resultingClass = klass
        }
        return resultingClass ?: typeAliases.firstOrNull()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        recordLookup(name, location)
        return functionDescriptors(name)
    }

    private fun doGetFunctions(name: Name): Collection<SimpleFunctionDescriptor> {
        val result = Sets.newLinkedHashSet<SimpleFunctionDescriptor>()

        val declarations = declarationProvider.getFunctionDeclarations(name)
        for (functionDeclaration in declarations) {
            result.add(c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(functionDeclaration),
                    functionDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration)))
        }

        getNonDeclaredFunctions(name, result)

        return result.toList()
    }

    protected abstract fun getScopeForMemberDeclarationResolution(declaration: KtDeclaration): LexicalScope

    protected abstract fun getScopeForInitializerResolution(declaration: KtDeclaration): LexicalScope

    protected abstract fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>)

    protected abstract fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>)

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        recordLookup(name, location)
        return propertyDescriptors(name)
    }

    fun doGetProperties(name: Name): Collection<PropertyDescriptor> {
        val result = LinkedHashSet<PropertyDescriptor>()

        val declarations = declarationProvider.getPropertyDeclarations(name)
        for (propertyDeclaration in declarations) {
            val propertyDescriptor = c.descriptorResolver.resolvePropertyDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(propertyDeclaration),
                    getScopeForInitializerResolution(propertyDeclaration),
                    propertyDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(propertyDeclaration))
            result.add(propertyDescriptor)
        }

        for (entry in declarationProvider.getDestructuringDeclarationsEntries(name)) {
            val propertyDescriptor = c.descriptorResolver.resolveDestructuringDeclarationEntryAsProperty(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(entry),
                    getScopeForInitializerResolution(entry),
                    entry,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(entry))
            result.add(propertyDescriptor)
        }

        getNonDeclaredProperties(name, result)

        return result.toList()
    }

    protected abstract fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>)

    protected fun getContributedTypeAliasDescriptors(name: Name, location: LookupLocation): Collection<TypeAliasDescriptor> {
        recordLookup(name, location)
        return typeAliasDescriptors(name)
    }

    private fun doGetTypeAliases(name: Name): Collection<TypeAliasDescriptor> =
            declarationProvider.getTypeAliasDeclarations(name).map { ktTypeAlias ->
                c.descriptorResolver.resolveTypeAliasDescriptor(
                        thisDescriptor,
                        getScopeForMemberDeclarationResolution(ktTypeAlias),
                        ktTypeAlias,
                        trace)
            }.toList()

    protected fun computeDescriptorsFromDeclaredElements(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ): List<DeclarationDescriptor> {
        val declarations = declarationProvider.getDeclarations(kindFilter, nameFilter)
        val result = LinkedHashSet<DeclarationDescriptor>(declarations.size)
        for (declaration in declarations) {
            when (declaration) {
                is KtClassOrObject -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(classDescriptors(name))
                    }
                }
                is KtFunction -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedFunctions(name, location))
                    }
                }
                is KtProperty -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedVariables(name, location))
                    }
                }
                is KtParameter -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedVariables(name, location))
                    }
                }
                is KtTypeAlias -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedTypeAliasDescriptors(name, location))
                    }
                }
                is KtScript -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(classDescriptors(name))
                    }
                }
                is KtDestructuringDeclaration -> {
                    // MultiDeclarations are not supported on global level
                }
                else -> throw IllegalArgumentException("Unsupported declaration kind: " + declaration)
            }
        }
        return result.toList()
    }

    abstract fun recordLookup(name: Name, from: LookupLocation)

    // Do not change this, override in concrete subclasses:
    // it is very easy to compromise laziness of this class, and fail all the debugging
    // a generic implementation can't do this properly
    abstract override fun toString(): String

    fun toProviderString() = (declarationProvider as? AbstractPsiBasedDeclarationProvider)?.toInfoString()
                             ?: declarationProvider.toString()

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("thisDescriptor = ", thisDescriptor)

        p.popIndent()
        p.println("}")
    }
}
