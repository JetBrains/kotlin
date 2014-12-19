/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.plugin.stubindex.*
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import com.intellij.openapi.project.Project
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import org.jetbrains.jet.plugin.util.substituteExtensionIfCallable
import org.jetbrains.jet.plugin.util.CallType
import org.jetbrains.jet.plugin.codeInsight.ReferenceVariantsHelper
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.jet.plugin.util.getImplicitReceiversWithInstance

public class KotlinIndicesHelper(
        private val project: Project,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val scope: GlobalSearchScope,
        private val moduleDescriptor: ModuleDescriptor,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    public fun getTopLevelCallablesByName(name: String, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return listOf()

        val declarations = HashSet<JetNamedDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(JetFunctionShortNameIndex.getInstance(), name)
        declarations.addTopLevelNonExtensionCallablesByName(JetPropertyShortNameIndex.getInstance(), name)
        return declarations.flatMap {
            if (it.getContainingJetFile().isCompiled()) {
                val importDirective = JetPsiFactory(project).createImportDirective(it.getFqName().asString())
                analyzeImportReference(importDirective, resolutionScope, BindingTraceContext(), moduleDescriptor).filterIsInstance<CallableDescriptor>()
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

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val sourceNames = JetTopLevelFunctionFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertyFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) }

        val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return listOf()

        return allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it, context, jetScope).filter(visibilityFilter) }
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

            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, receiverExpression] ?: return

            val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

            matchingNames.flatMapTo(this) {
                findSuitableExtensions(it, index, receiverValue, dataFlowInfo, callType, resolutionScope, moduleDescriptor, bindingContext)
            }
        }
        else {
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression] ?: return

            for (receiver in resolutionScope.getImplicitReceiversWithInstance()) {
                matchingNames.flatMapTo(this) {
                    findSuitableExtensions(it, index, receiver.getValue(), dataFlowInfo, CallType.NORMAL, resolutionScope, moduleDescriptor, bindingContext)
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
                                       resolutionScope: JetScope,
                                       module: ModuleDescriptor,
                                       bindingContext: BindingContext): Stream<CallableDescriptor> {
        val fqnString = callableFQN.asString()
        val extensions = index.get(fqnString, project, scope).filter { it.getReceiverTypeReference() != null }
        val descriptors = if (extensions.any { it.getContainingJetFile().isCompiled() } ) {
            val importDirective = JetPsiFactory(project).createImportDirective(fqnString)
            analyzeImportReference(importDirective, resolutionScope, BindingTraceContext(), module)
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

    private fun findTopLevelCallables(fqName: FqName, context: JetExpression, jetScope: JetScope): Collection<CallableDescriptor> {
        val importDirective = JetPsiFactory(context.getProject()).createImportDirective(ImportPath(fqName, false))
        val allDescriptors = analyzeImportReference(importDirective, jetScope, BindingTraceContext(), moduleDescriptor)
        return allDescriptors.filterIsInstance<CallableDescriptor>().filter { it.getExtensionReceiverParameter() == null }
    }

    private fun analyzeImportReference(
            importDirective: JetImportDirective, scope: JetScope, trace: BindingTrace, module: ModuleDescriptor
    ): Collection<DeclarationDescriptor> {
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace,
                                                                    module, LookupMode.EVERYTHING)
    }
}