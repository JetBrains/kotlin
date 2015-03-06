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

package org.jetbrains.kotlin.idea.caches

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import com.intellij.openapi.project.Project
import java.util.HashSet
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstance
import org.jetbrains.kotlin.idea.stubindex.*

public class KotlinIndicesHelper(
        private val project: Project,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val scope: GlobalSearchScope,
        private val moduleDescriptor: ModuleDescriptor,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    public fun getTopLevelCallablesByName(name: String): Collection<CallableDescriptor> {
        val declarations = HashSet<JetNamedDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(JetFunctionShortNameIndex.getInstance(), name)
        declarations.addTopLevelNonExtensionCallablesByName(JetPropertyShortNameIndex.getInstance(), name)
        return declarations.flatMap {
            if (it.getContainingJetFile().isCompiled()) {
                analyzeImportReference(it.getFqName()!!).filterIsInstance<CallableDescriptor>()
            }
            else {
                (resolutionFacade.resolveToDescriptor(it) as? CallableDescriptor).singletonOrEmptyList()
            }
        }.filter { it.getExtensionReceiverParameter() == null && visibilityFilter(it) }
    }

    private fun MutableSet<JetNamedDeclaration>.addTopLevelNonExtensionCallablesByName(
            index: StringStubIndexExtension<out JetCallableDeclaration>,
            name: String
    ) {
        index.get(name, project, scope).filterTo(this) { it.getParent() is JetFile && it.getReceiverTypeReference() == null }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean): Collection<CallableDescriptor> {
        val sourceNames = JetTopLevelFunctionFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertyFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) }
        return allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it).filter(visibilityFilter) }
    }

    public fun getCallableExtensions(nameFilter: (String) -> Boolean, expression: JetSimpleNameExpression): Collection<CallableDescriptor> {
        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

        val functionsIndex = JetTopLevelFunctionFqnNameIndex.getInstance()
        val propertiesIndex = JetTopLevelPropertyFqnNameIndex.getInstance()

        val sourceFunctionNames = functionsIndex.getAllKeys(project).stream().map { FqName(it) }
        val sourcePropertyNames = propertiesIndex.getAllKeys(project).stream().map { FqName(it) }

        val result = HashSet<CallableDescriptor>()
        result.fqNamesToSuitableExtensions(sourceFunctionNames, nameFilter, functionsIndex, expression, bindingContext, dataFlowInfo)
        result.fqNamesToSuitableExtensions(sourcePropertyNames, nameFilter, propertiesIndex, expression, bindingContext, dataFlowInfo)
        return result
    }

    private fun MutableCollection<CallableDescriptor>.fqNamesToSuitableExtensions(
            fqNames: Stream<FqName>,
            nameFilter: (String) -> Boolean,
            index: StringStubIndexExtension<out JetCallableDeclaration>,
            expression: JetSimpleNameExpression,
            bindingContext: BindingContext,
            dataFlowInfo: DataFlowInfo) {
        val matchingNames = fqNames.filter { nameFilter(it.shortName().asString()) }

        val receiverPair = ReferenceVariantsHelper.getExplicitReceiverData(expression)
        if (receiverPair != null) {
            val (receiverExpression, callType) = receiverPair

            val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, receiverExpression]
            if (expressionType == null || expressionType.isError()) return

            val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

            matchingNames.flatMapTo(this) {
                findSuitableExtensions(it, index, receiverValue, dataFlowInfo, callType, bindingContext)
            }
        }
        else {
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression] ?: return

            for (receiver in resolutionScope.getImplicitReceiversWithInstance()) {
                matchingNames.flatMapTo(this) {
                    findSuitableExtensions(it, index, receiver.getValue(), dataFlowInfo, CallType.NORMAL, bindingContext)
                }
            }
        }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(callableFQN: FqName,
                                       index: StringStubIndexExtension<out JetCallableDeclaration>,
                                       receiverValue: ReceiverValue,
                                       dataFlowInfo: DataFlowInfo,
                                       callType: CallType,
                                       bindingContext: BindingContext): Stream<CallableDescriptor> {
        val extensions = index.get(callableFQN.asString(), project, scope).filter { it.getReceiverTypeReference() != null }
        val descriptors = if (extensions.any { it.getContainingJetFile().isCompiled() } ) {
            analyzeImportReference(callableFQN)
                    .filterIsInstance<CallableDescriptor>()
                    .filter { it.getExtensionReceiverParameter() != null }
        }
        else extensions.map { resolutionFacade.resolveToDescriptor(it) as CallableDescriptor }
        return descriptors.stream()
                .filter(visibilityFilter)
                .flatMap { it.substituteExtensionIfCallable(receiverValue, callType, bindingContext, dataFlowInfo).stream() }
    }

    public fun getClassDescriptors(nameFilter: (String) -> Boolean, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).stream()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(it, kindFilter) }
    }

    private fun getClassDescriptorsByFQName(classFQName: FqName, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        val declarations = JetFullClassNameIndex.getInstance()[classFQName.asString(), project, scope]

        if (declarations.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return listOf()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, classFQName) { kindFilter(it.getKind()) }
                .filter(visibilityFilter)
    }

    private fun findTopLevelCallables(fqName: FqName): Collection<CallableDescriptor> {
        return analyzeImportReference(fqName)
                .filterIsInstance<CallableDescriptor>()
                .filter { it.getExtensionReceiverParameter() == null }
    }

    private fun analyzeImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
        val importDirective = JetPsiFactory(project).createImportDirective(ImportPath(fqName, false))
        val scope = JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor)
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, null, BindingTraceContext(), LookupMode.EVERYTHING)
    }
}
