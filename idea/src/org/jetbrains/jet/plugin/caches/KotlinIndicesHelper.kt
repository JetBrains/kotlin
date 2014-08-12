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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.plugin.stubindex.JetTopLevelObjectShortNameIndex
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.plugin.stubindex.JetTopLevelNonExtensionFunctionShortNameIndex
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import java.util.ArrayList
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex
import org.jetbrains.jet.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import com.intellij.openapi.project.Project

public class KotlinIndicesHelper(private val project: Project) {
    public fun getTopLevelObjects(nameFilter: (String) -> Boolean, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val allObjectNames = JetTopLevelObjectShortNameIndex.getInstance().getAllKeys(project).stream() +
                JetFromJavaDescriptorHelper.getPossiblePackageDeclarationsNames(project, scope).stream()
        return allObjectNames
                .filter(nameFilter)
                .toSet()
                .flatMap { getTopLevelObjectsByName(it, resolveSession, scope) }
    }

    private fun getTopLevelObjectsByName(name: String, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val result = hashSetOf<ClassDescriptor>()

        val topObjects = JetTopLevelObjectShortNameIndex.getInstance().get(name, project, scope)
        for (objectDeclaration in topObjects) {
            val fqName = objectDeclaration.getFqName() ?: error("Local object declaration in JetTopLevelShortObjectNameIndex:${objectDeclaration.getText()}")
            result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession, fqName, ResolveSessionUtils.SINGLETON_FILTER))
        }

        for (psiClass in JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, scope)) {
            val qualifiedName = psiClass.getQualifiedName()
            if (qualifiedName != null) {
                result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession, FqName(qualifiedName), ResolveSessionUtils.SINGLETON_FILTER))
            }
        }

        return result
    }

    public fun getTopLevelFunctionDescriptorsByName(name: String, context: JetExpression /*TODO: to be dropped*/, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<FunctionDescriptor> {

        val jetScope = resolveSession.resolveToElement(context).get(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()

        val result = hashSetOf<FunctionDescriptor>()

        //TODO: this code is temporary and is to be dropped when compiled top level functions are indexed
        val identifier = Name.identifier(name)
        for (fqName in JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false)) {
            if (fqName.lastSegmentIs(identifier)) {
                findTopLevelCallables(fqName, context, jetScope, resolveSession).filterIsInstanceTo(result, javaClass<FunctionDescriptor>())
            }
        }

        result.addSourceTopLevelFunctions(name, resolveSession, scope)

        return result
    }

    private fun MutableCollection<in FunctionDescriptor>.addSourceTopLevelFunctions(name: String, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope) {
        val identifier = Name.identifier(name)
        val affectedPackages = JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().get(name, project, scope)
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

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean, context: JetExpression /*TODO: to be dropped*/, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()

        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false).stream()

        val jetScope = resolveSession.resolveToElement(context).get(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()
        allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMapTo(result) { findTopLevelCallables(it, context, jetScope, resolveSession) }

        return result
    }

    public fun getCallableExtensions(nameFilter: (String) -> Boolean,
                                     expression: JetSimpleNameExpression,
                                     resolveSession: ResolveSessionForBodies,
                                     scope: GlobalSearchScope): Collection<DeclarationDescriptor> {
        val context = resolveSession.resolveToElement(expression)
        val receiverExpression = expression.getReceiverExpression() ?: return listOf()
        val expressionType = context.get<JetExpression, JetType>(BindingContext.EXPRESSION_TYPE, receiverExpression)
        val jetScope = context.get(BindingContext.RESOLUTION_SCOPE, receiverExpression)

        if (expressionType == null || jetScope == null || expressionType.isError()) {
            return listOf()
        }

        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, true).stream()

        // Iterate through the function with attempt to resolve found functions
        return allFqNames
                .filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { ExpressionTypingUtils.canFindSuitableCall(it, receiverExpression, expressionType, jetScope, resolveSession.getModuleDescriptor()) }
    }

    public fun getClassDescriptors(nameFilter: (String) -> Boolean, analyzer: KotlinCodeAnalyzer, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).stream()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(analyzer, it, scope) }
    }

    private fun getClassDescriptorsByFQName(analyzer: KotlinCodeAnalyzer, classFQName: FqName, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val jetClassOrObjects = JetFullClassNameIndex.getInstance().get(classFQName.asString(), project, scope)

        if (jetClassOrObjects.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return listOf()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassDescriptorsByFqName(analyzer, classFQName)
    }

    private fun findTopLevelCallables(fqName: FqName, context: JetExpression, jetScope: JetScope, resolveSession: ResolveSessionForBodies): Collection<DeclarationDescriptor> {
        val importDirective = JetPsiFactory(context.getProject()).createImportDirective(ImportPath(fqName, false))
        val allDescriptors = QualifiedExpressionResolver().analyseImportReference(importDirective, jetScope, BindingTraceContext(), resolveSession.getModuleDescriptor())
        return allDescriptors.filterIsInstance(javaClass<CallableDescriptor>()).filter { it.getReceiverParameter() == null }
    }

    public fun getClassDescriptorsByName(name: String, analyzer: KotlinCodeAnalyzer, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        return JetShortNamesCache.getKotlinInstance(project).getClassesByName(name, scope).flatMap { ResolveSessionUtils.getClassDescriptorsByFqName(analyzer, FqName(it.getQualifiedName()!!)) }
    }
}