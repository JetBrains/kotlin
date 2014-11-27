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
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
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
import org.jetbrains.jet.plugin.util.extensionsUtils.isExtensionCallable

public class KotlinIndicesHelper(
        private val project: Project,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val scope: GlobalSearchScope,
        private val moduleDescriptor: ModuleDescriptor,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
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
            result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, fqName, ResolveSessionUtils.SINGLETON_FILTER))
        }

        for (psiClass in JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, scope)) {
            val qualifiedName = psiClass.getQualifiedName()
            if (qualifiedName != null) {
                result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, FqName(qualifiedName), ResolveSessionUtils.SINGLETON_FILTER))
            }
        }

        return result.filter(visibilityFilter)
    }

    public fun getTopLevelCallablesByName(name: String, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return listOf()

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
            val packageDescriptor = moduleDescriptor.getPackage(affectedPackage)
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
            val packageDescriptor = moduleDescriptor.getPackage(affectedPackage)
                    ?: error("There's a property in stub index with invalid package: $affectedPackage")
            addAll(packageDescriptor.getMemberScope().getProperties(identifier))
        }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false).stream()

        val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return listOf()

        return allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it, context, jetScope).filter(visibilityFilter) }
    }

    public fun getCallableExtensions(nameFilter: (String) -> Boolean, expression: JetSimpleNameExpression): Collection<CallableDescriptor> {
        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

        val functionsIndex = JetTopLevelFunctionsFqnNameIndex.getInstance()
        val propertiesIndex = JetTopLevelPropertiesFqnNameIndex.getInstance()

        val sourceFunctionNames = functionsIndex.getAllKeys(project).stream().map { FqName(it) }
        val sourcePropertyNames = propertiesIndex.getAllKeys(project).stream().map { FqName(it) }
        val compiledFqNames = JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, true).stream()

        val result = HashSet<CallableDescriptor>()
        result.fqNamesToSuitableExtensions(sourceFunctionNames, nameFilter, functionsIndex, expression, bindingContext, dataFlowInfo)
        result.fqNamesToSuitableExtensions(sourcePropertyNames, nameFilter, propertiesIndex, expression, bindingContext, dataFlowInfo)
        result.fqNamesToSuitableExtensions(compiledFqNames, nameFilter, null, expression, bindingContext, dataFlowInfo)
        return result
    }

    private fun MutableCollection<CallableDescriptor>.fqNamesToSuitableExtensions(
            fqNames: Stream<FqName>,
            nameFilter: (String) -> Boolean,
            index: StringStubIndexExtension<out JetCallableDeclaration>?,
            expression: JetSimpleNameExpression,
            bindingContext: BindingContext,
            dataFlowInfo: DataFlowInfo) {
        val matchingNames = fqNames.filter { nameFilter(it.shortName().asString()) }

        val receiverExpression = expression.getReceiverExpression()
        if (receiverExpression != null) {
            val isInfixCall = expression.getParent() is JetBinaryExpression

            val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, receiverExpression]
            if (expressionType == null || expressionType.isError()) return

            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, receiverExpression] ?: return

            val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

            matchingNames.flatMapTo(this) {
                findSuitableExtensions(it, index, receiverValue, dataFlowInfo, isInfixCall, resolutionScope, moduleDescriptor, bindingContext).stream()
            }
        }
        else {
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression] ?: return

            for (receiver in resolutionScope.getImplicitReceiversHierarchy()) {
                matchingNames.flatMapTo(this) {
                    findSuitableExtensions(it, index, receiver.getValue(), dataFlowInfo, false, resolutionScope, moduleDescriptor, bindingContext).stream()
                }
            }
        }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(callableFQN: FqName,
                                       index: StringStubIndexExtension<out JetCallableDeclaration>?,
                                       receiverValue: ReceiverValue,
                                       dataFlowInfo: DataFlowInfo,
                                       isInfixCall: Boolean,
                                       resolutionScope: JetScope,
                                       module: ModuleDescriptor,
                                       bindingContext: BindingContext): Collection<CallableDescriptor> {
        val fqnString = callableFQN.asString()
        val descriptors = /* this code is temporarily disabled because taking descriptors from another resolve session causes duplicates and potentially other problems*/
        /*if (index != null) {
            index.get(fqnString, project, scope)
                    .filter { it.getReceiverTypeReference() != null }
                    .map { it.getLazyResolveSession().resolveToDescriptor(it) as CallableDescriptor }
        }
        else*/ run {
            val importDirective = JetPsiFactory(project).createImportDirective(fqnString)
            analyzeImportReference(importDirective, resolutionScope, BindingTraceContext(), module)
                    .filterIsInstance(javaClass<CallableDescriptor>())
                    .filter { it.getExtensionReceiverParameter() != null }
        }

        return descriptors.filter {
            visibilityFilter(it) && it.isExtensionCallable(receiverValue, isInfixCall, bindingContext, dataFlowInfo)
        }
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
        return allDescriptors.filterIsInstance(javaClass<CallableDescriptor>()).filter { it.getExtensionReceiverParameter() == null }
    }

    private fun analyzeImportReference(
            importDirective: JetImportDirective, scope: JetScope, trace: BindingTrace, module: ModuleDescriptor
    ): Collection<DeclarationDescriptor> {
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace,
                                                                    module, LookupMode.EVERYTHING)
    }
}