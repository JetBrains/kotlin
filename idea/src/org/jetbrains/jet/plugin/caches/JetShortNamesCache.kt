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

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.util.ArrayUtil
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.jet.asJava.JavaElementFinder
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.plugin.caches.resolve.IDELightClassGenerationSupport
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.stubindex.*

import java.util.*
import com.intellij.util.containers

/**
 * Will provide both java elements from kotlin context and some declarations special to kotlin.
 * All those declaration are planned to be used in completion.
 */
public class JetShortNamesCache(private val project: Project) : PsiShortNamesCache() {
    class object {
        public fun getKotlinInstance(project: Project): JetShortNamesCache {
            val extensions = Extensions.getArea(project).getExtensionPoint<PsiShortNamesCache>(PsiShortNamesCache.EP_NAME).getExtensions()
            for (extension in extensions) {
                if (extension is JetShortNamesCache) {
                    return extension as JetShortNamesCache
                }
            }
            throw IllegalStateException(javaClass<JetShortNamesCache>().getSimpleName() + " is not found for project " + project)
        }
    }

    /**
     * Return kotlin class names from project sources which should be visible from java.
     */
    override fun getAllClassNames(): Array<String> {
        val classNames = JetClassShortNameIndex.getInstance().getAllKeys(project)

        // package classes can not be indexed, since they have no explicit declarations
        val lightClassGenerationSupport = IDELightClassGenerationSupport.getInstanceForIDE(project)
        val packageClassShortNames = lightClassGenerationSupport.getAllPossiblePackageClasses(GlobalSearchScope.allScope(project)).keySet()
        classNames.addAll(packageClassShortNames)

        return ArrayUtil.toStringArray(classNames)
    }

    /**
     * Return class names form kotlin sources in given scope which should be visible as Java classes.
     */
    override fun getClassesByName(NonNls name: String, scope: GlobalSearchScope): Array<PsiClass> {
        val result = ArrayList<PsiClass>()

        val lightClassGenerationSupport = IDELightClassGenerationSupport.getInstanceForIDE(project)
        val packageClasses = lightClassGenerationSupport.getAllPossiblePackageClasses(scope)

        // package classes can not be indexed, since they have no explicit declarations
        val fqNames = packageClasses.get(name)
        if (!fqNames.isEmpty()) {
            for (fqName in fqNames) {
                val psiClass = JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope)
                if (psiClass != null) {
                    result.add(psiClass)
                }
            }
        }

        // Quick check for classes from getAllClassNames()
        val classOrObjects = JetClassShortNameIndex.getInstance().get(name, project, scope)
        if (classOrObjects.isEmpty()) {
            return result.copyToArray()
        }

        for (classOrObject in classOrObjects) {
            val fqName = classOrObject.getFqName()
            if (fqName != null) {
                assert(fqName.shortName().asString() == name) { "A declaration obtained from index has non-matching name:\n" + "in index: " + name + "\n" + "declared: " + fqName.shortName() + "(" + fqName + ")" }
                val psiClass = JavaElementFinder.getInstance(project).findClass(fqName.asString(), scope)
                if (psiClass != null) {
                    result.add(psiClass)
                }
            }
        }

        return result.copyToArray()
    }

    override fun getAllClassNames(destination: containers.HashSet<String>) {
        destination.addAll(Arrays.asList<String>(*getAllClassNames()))
    }

    /**
     * Get kotlin non-extension top-level function names. Method is allowed to give invalid names - all result should be
     * checked with getTopLevelFunctionDescriptorsByName().
     */
    public fun getAllTopLevelFunctionNames(): Collection<String> {
        return (JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().getAllKeys(project)
                + JetFromJavaDescriptorHelper.getPossiblePackageDeclarationsNames(project, GlobalSearchScope.allScope(project))).toSet()
    }

    public fun getAllTopLevelObjectNames(): Collection<String> {
        return (JetTopLevelObjectShortNameIndex.getInstance().getAllKeys(project)
                + JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, GlobalSearchScope.allScope(project)).map { it.getName()!! }).toSet()
    }

    public fun getTopLevelObjectsByName(name: String, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val result = hashSetOf<ClassDescriptor>()

        val topObjects = JetTopLevelObjectShortNameIndex.getInstance().get(name, project, scope)
        for (objectDeclaration in topObjects) {
            val fqName = objectDeclaration.getFqName() ?: error("Local object declaration in JetTopLevelShortObjectNameIndex:${objectDeclaration.getText()}")
            result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession, fqName, ResolveSessionUtils.SINGLETON_FILTER))
        }

        for (psiClass in JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, GlobalSearchScope.allScope(project))) {
            val qualifiedName = psiClass.getQualifiedName()
            if (qualifiedName != null) {
                result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession, FqName(qualifiedName), ResolveSessionUtils.SINGLETON_FILTER))
            }
        }

        return result
    }

    public fun getTopLevelFunctionDescriptorsByName(name: String, context: JetExpression /*TODO: to be dropped*/, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<FunctionDescriptor> {

        val jetScope = resolveSession.resolveToElement(context).get<JetExpression, JetScope>(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()

        val result = hashSetOf<FunctionDescriptor>()

        //TODO: this code is temporary and is to be dropped when compiled top level functions are indexed
        val identifier = Name.identifier(name)
        val topLevelFunctionFqNames = JetFromJavaDescriptorHelper.getTopLevelFunctionFqNames(project, scope, false)
        for (fqName in topLevelFunctionFqNames) {
            if (fqName.lastSegmentIs(identifier)) {
                val importDirective = JetPsiFactory(context.getProject()).createImportDirective(ImportPath(fqName, false))
                val declarationDescriptors = QualifiedExpressionResolver().analyseImportReference(importDirective, jetScope, BindingTraceContext(), resolveSession.getModuleDescriptor())
                for (declarationDescriptor in declarationDescriptors) {
                    if (declarationDescriptor is FunctionDescriptor) {
                        result.add(declarationDescriptor as FunctionDescriptor)
                    }
                }
            }
        }

        val affectedPackages = JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().get(name, project, scope)
                .map { it.getContainingFile() }
                .filterIsInstance(javaClass<JetFile>())
                .map { it.getPackageFqName() }
                .toSet()

        for (affectedPackage in affectedPackages) {
            val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(affectedPackage)
                    ?: error("There's a function in stub index with invalid package: $affectedPackage")
            result.addAll(packageDescriptor.getMemberScope().getFunctions(identifier))
        }

        return result
    }

    private fun getJetExtensionFunctionsByName(name: String, scope: GlobalSearchScope): Collection<PsiElement>
            = JetTopLevelExtensionFunctionShortNameIndex.getInstance().get(name, project, scope)

    // TODO: Make it work for properties
    public fun getJetCallableExtensions(nameFilter: (String) -> Boolean, expression: JetSimpleNameExpression, resolveSession: ResolveSessionForBodies, searchScope: GlobalSearchScope): Collection<DeclarationDescriptor> {
        val context = resolveSession.resolveToElement(expression)
        val receiverExpression = expression.getReceiverExpression() ?: return listOf()
        val expressionType = context.get<JetExpression, JetType>(BindingContext.EXPRESSION_TYPE, receiverExpression)
        val scope = context.get<JetExpression, JetScope>(BindingContext.RESOLUTION_SCOPE, receiverExpression)

        if (expressionType == null || scope == null || expressionType.isError()) {
            return listOf()
        }

        val functionFQNs = extensionFunctionsFromSourceFqNames(nameFilter, searchScope)

        JetFromJavaDescriptorHelper.getTopLevelFunctionFqNames(project, searchScope, true)
                .filterTo(functionFQNs) { nameFilter(it.shortName().asString()) }

        // Iterate through the function with attempt to resolve found functions
        return functionFQNs.flatMap { ExpressionTypingUtils.canFindSuitableCall(it, receiverExpression, expressionType, scope, resolveSession.getModuleDescriptor()) }
    }

    private fun extensionFunctionsFromSourceFqNames(nameFilter: (String) -> Boolean, searchScope: GlobalSearchScope): HashSet<FqName> {
        val extensionFunctionNames = HashSet(JetTopLevelExtensionFunctionShortNameIndex.getInstance().getAllKeys(project))

        val functionFQNs = HashSet<FqName>()

        // Collect all possible extension function qualified names
        for (name in extensionFunctionNames) {
            if (nameFilter(name)) {
                val extensionFunctions = getJetExtensionFunctionsByName(name, searchScope)

                for (extensionFunction in extensionFunctions) {
                    if (extensionFunction is JetNamedFunction) {
                        functionFQNs.add(extensionFunction.getFqName()!!)
                    }
                    else if (extensionFunction is PsiMethod) {
                        val functionFQN = JetFromJavaDescriptorHelper.getJetTopLevelDeclarationFQN(extensionFunction)
                        if (functionFQN != null) {
                            functionFQNs.add(functionFQN)
                        }
                    }
                }
            }
        }
        return functionFQNs
    }

    public fun getJetClassesDescriptors(acceptedShortNameCondition: (String) -> Boolean, analyzer: KotlinCodeAnalyzer, searchScope: GlobalSearchScope): Collection<ClassDescriptor> {
        val classDescriptors = ArrayList<ClassDescriptor>()

        for (fqName in JetFullClassNameIndex.getInstance().getAllKeys(project)) {
            val classFQName = FqName(fqName)
            if (acceptedShortNameCondition(classFQName.shortName().asString())) {
                classDescriptors.addAll(getJetClassesDescriptorsByFQName(analyzer, classFQName, searchScope))
            }
        }

        return classDescriptors
    }

    private fun getJetClassesDescriptorsByFQName(analyzer: KotlinCodeAnalyzer, classFQName: FqName, searchScope: GlobalSearchScope): Collection<ClassDescriptor> {
        val jetClassOrObjects = JetFullClassNameIndex.getInstance().get(classFQName.asString(), project, searchScope)

        if (jetClassOrObjects.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return listOf()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassDescriptorsByFqName(analyzer, classFQName)
    }

    override fun getMethodsByName(NonNls name: String, scope: GlobalSearchScope): Array<PsiMethod>
            = array()

    override fun getMethodsByNameIfNotMoreThan(NonNls name: String, scope: GlobalSearchScope, maxCount: Int): Array<PsiMethod>
            = array()

    override fun getFieldsByNameIfNotMoreThan(NonNls s: String, scope: GlobalSearchScope, i: Int): Array<PsiField>
            = array()

    override fun processMethodsWithName(NonNls name: String, scope: GlobalSearchScope, processor: Processor<PsiMethod>): Boolean
            = ContainerUtil.process(getMethodsByName(name, scope), processor)

    override fun getAllMethodNames(): Array<String>
            = array()

    override fun getAllMethodNames(set: containers.HashSet<String>) {
        set.addAll(JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().getAllKeys(project))
    }

    override fun getFieldsByName(NonNls name: String, scope: GlobalSearchScope): Array<PsiField>
            = array()

    override fun getAllFieldNames(): Array<String>
            = array()

    override fun getAllFieldNames(set: containers.HashSet<String>) {
    }
}
