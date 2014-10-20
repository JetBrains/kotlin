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

import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.plugin.stubindex.*
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import com.intellij.openapi.project.Project
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo

public class KotlinIndicesHelper(private val project: Project,
                                 private val resolveSession: ResolveSessionForBodies,
                                 private val scope: GlobalSearchScope,
                                 private val visibilityFilter: (DeclarationDescriptor) -> Boolean) {
    public fun getTopLevelObjects(nameFilter: (String) -> Boolean): Collection<ClassDescriptor> {
        val allObjectNames = JetTopLevelObjectShortNameIndex.getInstance().getAllKeys(project).stream() +
                JetFromJavaDescriptorHelper.getPossiblePackageDeclarationsNames(project, scope).stream()
        return allObjectNames
                .filter(nameFilter)
                .toSet()
                .flatMap { getTopLevelObjectsByName(it) }
    }

    private fun getTopLevelObjectsByName(name: String): Collection<ClassDescriptor> {
        val result = hashSetOf<ClassDescriptor>()

        val topObjects = JetTopLevelObjectShortNameIndex.getInstance().get(name, project, scope)
        for (objectDeclaration in topObjects) {
            val fqName = objectDeclaration.getFqName() ?: error("Local object declaration in JetTopLevelShortObjectNameIndex:${objectDeclaration.getText()}")
            result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession.getModuleDescriptor(), fqName, ResolveSessionUtils.SINGLETON_FILTER))
        }

        for (psiClass in JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, scope)) {
            val qualifiedName = psiClass.getQualifiedName()
            if (qualifiedName != null) {
                result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession.getModuleDescriptor(), FqName(qualifiedName), ResolveSessionUtils.SINGLETON_FILTER))
            }
        }

        return result.filter(visibilityFilter)
    }

    public fun getTopLevelCallablesByName(name: String, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val jetScope = resolveSession.resolveToElement(context).get(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()

        val result = HashSet<CallableDescriptor>()

        //TODO: this code is temporary and is to be dropped when compiled top level functions are indexed
        val identifier = Name.identifier(name)
        for (fqName in JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false)) {
            if (fqName.lastSegmentIs(identifier)) {
                result.addAll(findTopLevelCallables(fqName, context, jetScope))
            }
        }

        result.addSourceTopLevelFunctions(name)
        result.addSourceTopLevelProperties(name)

        return result.filter(visibilityFilter)
    }

    private fun MutableCollection<in FunctionDescriptor>.addSourceTopLevelFunctions(name: String) {
        val identifier = Name.identifier(name)
        val affectedPackages = JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().get(name, project, scope)
                .stream()
                .map { it.getContainingFile() }
                .filterIsInstance(javaClass<JetFile>())
                .map { it.getPackageFqName() }
                .toSet()

        for (affectedPackage in affectedPackages) {
            val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(affectedPackage)
                    ?: error("There's a function in stub index with invalid package: $affectedPackage")
            addAll(packageDescriptor.getMemberScope().getFunctions(identifier))
        }
    }

    private fun MutableCollection<in PropertyDescriptor>.addSourceTopLevelProperties(name: String) {
        val identifier = Name.identifier(name)
        val affectedPackages = JetTopLevelNonExtensionPropertyShortNameIndex.getInstance().get(name, project, scope)
                .stream()
                .map { it.getContainingFile() }
                .filterIsInstance(javaClass<JetFile>())
                .map { it.getPackageFqName() }
                .toSet()

        for (affectedPackage in affectedPackages) {
            val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(affectedPackage)
                    ?: error("There's a property in stub index with invalid package: $affectedPackage")
            addAll(packageDescriptor.getMemberScope().getProperties(identifier))
        }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false).stream()

        val jetScope = resolveSession.resolveToElement(context).get(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()

        return allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it, context, jetScope) }
    }

    public fun getCallableExtensions(nameFilter: (String) -> Boolean, expression: JetSimpleNameExpression): Collection<CallableDescriptor> {
        val bindingContext = resolveSession.resolveToElement(expression)
        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() +
                          JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, true).stream()
        val matchingFqNames = allFqNames.filter { nameFilter(it.shortName().asString()) }.toSet()

        val receiverExpression = expression.getReceiverExpression()
        if (receiverExpression != null) {
            val isInfixCall = expression.getParent() is JetBinaryExpression

            val expressionType = bindingContext.get<JetExpression, JetType>(BindingContext.EXPRESSION_TYPE, receiverExpression)
            if (expressionType == null || expressionType.isError()) return listOf()

            val resolutionScope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, receiverExpression) ?: return listOf()

            val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

            return matchingFqNames.flatMap {
                findSuitableExtensions(it, receiverValue, dataFlowInfo, isInfixCall, resolutionScope, resolveSession.getModuleDescriptor(), bindingContext)
            }
        }
        else {
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()

            val result = HashSet<CallableDescriptor>()
            for (receiver in resolutionScope.getImplicitReceiversHierarchy()) {
                matchingFqNames.flatMapTo(result) {
                    findSuitableExtensions(it, receiver.getValue(), dataFlowInfo, false, resolutionScope, resolveSession.getModuleDescriptor(), bindingContext)
                }
            }
            return result
        }
    }


    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(callableFQN: FqName,
                                       receiverValue: ReceiverValue,
                                       dataFlowInfo: DataFlowInfo,
                                       isInfixCall: Boolean,
                                       scope: JetScope,
                                       module: ModuleDescriptor,
                                       bindingContext: BindingContext): List<CallableDescriptor> {
        val importDirective = JetPsiFactory(project).createImportDirective(callableFQN.asString())
        val declarationDescriptors = analyzeImportReference(importDirective, scope, BindingTraceContext(), module)

        return declarationDescriptors
                .filterIsInstance(javaClass<CallableDescriptor>())
                .filter { it.getExtensionReceiverParameter() != null &&
                          visibilityFilter(it) &&
                          ExpressionTypingUtils.checkIsExtensionCallable(receiverValue, it, isInfixCall, bindingContext, dataFlowInfo) }
    }

    public fun getClassDescriptors(nameFilter: (String) -> Boolean): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).stream()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(it) }
    }

    private fun getClassDescriptorsByFQName(classFQName: FqName): Collection<ClassDescriptor> {
        val jetClassOrObjects = JetFullClassNameIndex.getInstance().get(classFQName.asString(), project, scope)

        if (jetClassOrObjects.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return listOf()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassDescriptorsByFqName(resolveSession.getModuleDescriptor(), classFQName).filter(visibilityFilter)
    }

    private fun findTopLevelCallables(fqName: FqName, context: JetExpression, jetScope: JetScope): Collection<CallableDescriptor> {
        val importDirective = JetPsiFactory(context.getProject()).createImportDirective(ImportPath(fqName, false))
        val allDescriptors = analyzeImportReference(importDirective, jetScope, BindingTraceContext(), resolveSession.getModuleDescriptor())
        return allDescriptors.filterIsInstance(javaClass<CallableDescriptor>()).filter { it.getExtensionReceiverParameter() == null && visibilityFilter(it) }
    }

    private fun analyzeImportReference(
            importDirective: JetImportDirective, scope: JetScope, trace: BindingTrace, module: ModuleDescriptor
    ): Collection<DeclarationDescriptor> {
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace,
                                                                    module, LookupMode.EVERYTHING)
    }
}