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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import com.google.common.collect.Sets
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AnnotationResolver
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.ScriptNameUtil
import org.jetbrains.kotlin.resolve.lazy.data.JetScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.utils.Printer

import java.util.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.toReadOnlyList
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext

public abstract class AbstractLazyMemberScope<D : DeclarationDescriptor, DP : DeclarationProvider> protected(
        protected val c: LazyClassContext,
        protected val declarationProvider: DP,
        protected val thisDescriptor: D,
        protected val trace: BindingTrace) : JetScope {

    protected val storageManager: StorageManager = c.storageManager
    private val classDescriptors: MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> = storageManager.createMemoizedFunction { resolveClassDescriptor(it) }
    private val functionDescriptors: MemoizedFunctionToNotNull<Name, Collection<FunctionDescriptor>> = storageManager.createMemoizedFunction { doGetFunctions(it) }
    private val propertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> = storageManager.createMemoizedFunction { doGetProperties(it) }

    private fun resolveClassDescriptor(name: Name): List<ClassDescriptor> {
        return declarationProvider.getClassOrObjectDeclarations(name).map {
            // SCRIPT: Creating a script class
            if (it is JetScriptInfo)
                LazyScriptClassDescriptor(c as ResolveSession, thisDescriptor, name, it)
            else
                LazyClassDescriptor(c, thisDescriptor, name, it)
        }.toReadOnlyList()
    }

    override fun getContainingDeclaration() = thisDescriptor

    override fun getClassifier(name: Name): ClassDescriptor? = classDescriptors(name).firstOrNull()

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = functionDescriptors(name)

    private fun doGetFunctions(name: Name): Collection<FunctionDescriptor> {
        val result = Sets.newLinkedHashSet<FunctionDescriptor>()

        val declarations = declarationProvider.getFunctionDeclarations(name)
        for (functionDeclaration in declarations) {
            val resolutionScope = getScopeForMemberDeclarationResolution(functionDeclaration)
            result.add(c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    resolutionScope,
                    functionDeclaration,
                    trace,
                    c.scopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration)))
        }

        getNonDeclaredFunctions(name, result)

        return result.toReadOnlyList()
    }

    protected abstract fun getScopeForMemberDeclarationResolution(declaration: JetDeclaration): JetScope

    protected abstract fun getNonDeclaredFunctions(name: Name, result: MutableSet<FunctionDescriptor>)

    override fun getProperties(name: Name): Collection<VariableDescriptor> = propertyDescriptors(name)

    public fun doGetProperties(name: Name): Collection<VariableDescriptor> {
        val result = LinkedHashSet<VariableDescriptor>()

        val declarations = declarationProvider.getPropertyDeclarations(name)
        for (propertyDeclaration in declarations) {
            val resolutionScope = getScopeForMemberDeclarationResolution(propertyDeclaration)
            val propertyDescriptor = c.descriptorResolver.resolvePropertyDescriptor(
                    thisDescriptor,
                    resolutionScope,
                    propertyDeclaration,
                    trace,
                    c.scopeProvider.getOuterDataFlowInfoForDeclaration(propertyDeclaration))
            result.add(propertyDescriptor)
            AnnotationResolver.resolveAnnotationsArguments(propertyDescriptor.getAnnotations(), trace)
        }

        getNonDeclaredProperties(name, result)

        return result.toReadOnlyList()
    }

    protected abstract fun getNonDeclaredProperties(name: Name, result: MutableSet<VariableDescriptor>)

    override fun getLocalVariable(name: Name): VariableDescriptor? = null

    override fun getDeclarationsByLabel(labelName: Name) = setOf<DeclarationDescriptor>()

    protected fun computeDescriptorsFromDeclaredElements(kindFilter: DescriptorKindFilter,
                                                         nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val declarations = declarationProvider.getDeclarations(kindFilter, nameFilter)
        val result = LinkedHashSet<DeclarationDescriptor>(declarations.size())
        for (declaration in declarations) {
            if (declaration is JetClassOrObject) {
                val name = declaration.getNameAsSafeName()
                if (nameFilter(name)) {
                    result.addAll(classDescriptors(name))
                }
            }
            else if (declaration is JetFunction) {
                val name = declaration.getNameAsSafeName()
                if (nameFilter(name)) {
                    result.addAll(getFunctions(name))
                }
            }
            else if (declaration is JetProperty) {
                val name = declaration.getNameAsSafeName()
                if (nameFilter(name)) {
                    result.addAll(getProperties(name))
                }
            }
            else if (declaration is JetParameter) {
                val name = declaration.getNameAsSafeName()
                if (nameFilter(name)) {
                    result.addAll(getProperties(name))
                }
            }
            else if (declaration is JetScript) {
                val name = ScriptNameUtil.classNameForScript(declaration).shortName()
                if (nameFilter(name)) {
                    result.addAll(classDescriptors(name))
                }
            }
            else if (declaration is JetTypedef || declaration is JetMultiDeclaration) {
                // Do nothing for typedefs as they are not supported.
                // MultiDeclarations are not supported on global level too.
            }
            else {
                throw IllegalArgumentException("Unsupported declaration kind: " + declaration)
            }
        }
        return result.toReadOnlyList()
    }

    override fun getImplicitReceiversHierarchy() = listOf<ReceiverParameterDescriptor>()

    // Do not change this, override in concrete subclasses:
    // it is very easy to compromise laziness of this class, and fail all the debugging
    // a generic implementation can't do this properly
    abstract override fun toString(): String

    override fun getOwnDeclaredDescriptors() = getDescriptors()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("thisDescriptor = ", thisDescriptor)

        p.popIndent()
        p.println("}")
    }
}
