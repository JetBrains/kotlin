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

package org.jetbrains.kotlin.resolve

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import java.util.*

class LazyTopDownAnalyzer(
        private val trace: BindingTrace,
        private val declarationResolver: DeclarationResolver,
        private val overrideResolver: OverrideResolver,
        private val overloadResolver: OverloadResolver,
        private val varianceChecker: VarianceChecker,
        private val moduleDescriptor: ModuleDescriptor,
        private val lazyDeclarationResolver: LazyDeclarationResolver,
        private val bodyResolver: BodyResolver,
        private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
        private val fileScopeProvider: FileScopeProvider,
        private val declarationScopeProvider: DeclarationScopeProvider,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val identifierChecker: IdentifierChecker,
        private val languageFeatureSettings: LanguageFeatureSettings
) {
    fun analyzeDeclarations(topDownAnalysisMode: TopDownAnalysisMode, declarations: Collection<PsiElement>, outerDataFlowInfo: DataFlowInfo): TopDownAnalysisContext {

        val c = TopDownAnalysisContext(topDownAnalysisMode, outerDataFlowInfo, declarationScopeProvider)

        val topLevelFqNames = HashMultimap.create<FqName, KtElement>()

        val properties = ArrayList<KtProperty>()
        val functions = ArrayList<KtNamedFunction>()
        val typeAliases = ArrayList<KtTypeAlias>()

        // fill in the context
        for (declaration in declarations) {
            declaration.accept(object : KtVisitorVoid() {
                private fun registerDeclarations(declarations: List<KtDeclaration>) {
                    for (jetDeclaration in declarations) {
                        jetDeclaration.accept(this)
                    }
                }

                override fun visitDeclaration(dcl: KtDeclaration) {
                    throw IllegalArgumentException("Unsupported declaration: " + dcl + " " + dcl.text)
                }

                override fun visitScript(script: KtScript) {
                    c.scripts.put(
                            script,
                            lazyDeclarationResolver.getScriptDescriptor(script, KotlinLookupLocation(script)) as LazyScriptDescriptor
                    )
                    registerDeclarations(script.declarations)
                }

                override fun visitKtFile(file: KtFile) {
                    DescriptorResolver.registerFileInPackage(trace, file)
                    registerDeclarations(file.declarations)
                    val packageDirective = file.packageDirective
                    assert(file.isScript || packageDirective != null) { "No package in a non-script file: " + file }
                    packageDirective?.accept(this)
                    c.addFile(file)
                    topLevelFqNames.put(file.packageFqName, packageDirective)
                }

                override fun visitPackageDirective(directive: KtPackageDirective) {
                    directive.packageNames.forEach { identifierChecker.checkIdentifier(it.getIdentifier(), trace) }
                    qualifiedExpressionResolver.resolvePackageHeader(directive, moduleDescriptor, trace)
                }

                override fun visitImportDirective(importDirective: KtImportDirective) {
                    val importResolver = fileScopeProvider.getImportResolver(importDirective.getContainingKtFile())
                    importResolver.forceResolveImport(importDirective)
                }

                override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                    val location = if (classOrObject.isTopLevel()) KotlinLookupLocation(classOrObject) else NoLookupLocation.WHEN_RESOLVE_DECLARATION
                    val descriptor = lazyDeclarationResolver.getClassDescriptor(classOrObject, location) as ClassDescriptorWithResolutionScopes

                    c.declaredClasses.put(classOrObject, descriptor)
                    registerDeclarations(classOrObject.getDeclarations())
                    registerTopLevelFqName(topLevelFqNames, classOrObject, descriptor)

                    checkClassOrObjectDeclarations(classOrObject, descriptor)
                }

                private fun checkClassOrObjectDeclarations(classOrObject: KtClassOrObject, classDescriptor: ClassDescriptor) {
                    var companionObjectAlreadyFound = false
                    for (jetDeclaration in classOrObject.getDeclarations()) {
                        if (jetDeclaration is KtObjectDeclaration && jetDeclaration.isCompanion()) {
                            if (companionObjectAlreadyFound) {
                                trace.report(MANY_COMPANION_OBJECTS.on(jetDeclaration))
                            }
                            companionObjectAlreadyFound = true
                        }
                        else if (jetDeclaration is KtSecondaryConstructor) {
                            if (DescriptorUtils.isSingletonOrAnonymousObject(classDescriptor)) {
                                trace.report(CONSTRUCTOR_IN_OBJECT.on(jetDeclaration))
                            }
                            else if (classDescriptor.kind == ClassKind.INTERFACE) {
                                trace.report(CONSTRUCTOR_IN_INTERFACE.on(jetDeclaration))
                            }
                        }
                    }
                }

                override fun visitClass(klass: KtClass) {
                    visitClassOrObject(klass)
                    registerPrimaryConstructorParameters(klass)
                }

                private fun registerPrimaryConstructorParameters(klass: KtClass) {
                    for (jetParameter in klass.getPrimaryConstructorParameters()) {
                        if (jetParameter.hasValOrVar()) {
                            c.primaryConstructorParameterProperties.put(jetParameter, lazyDeclarationResolver.resolveToDescriptor(jetParameter) as PropertyDescriptor)
                        }
                    }
                }

                override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
                    c.secondaryConstructors.put(constructor, lazyDeclarationResolver.resolveToDescriptor(constructor) as ConstructorDescriptor)
                }

                override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                    visitClassOrObject(enumEntry)
                }

                override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                    visitClassOrObject(declaration)
                }

                override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer) {
                    val containerDescriptor = lazyDeclarationResolver.resolveToDescriptor(initializer.containingDeclaration) as ClassDescriptorWithResolutionScopes
                    c.anonymousInitializers.put(initializer, containerDescriptor)
                }

                override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
                    // Ignore: multi-declarations are only allowed locally
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    functions.add(function)
                }

                override fun visitProperty(property: KtProperty) {
                    properties.add(property)
                }

                override fun visitTypeAlias(typeAlias: KtTypeAlias) {
                    typeAliases.add(typeAlias)
                }
            })
        }

        createFunctionDescriptors(c, functions)

        createPropertyDescriptors(c, topLevelFqNames, properties)

        createTypeAliasDescriptors(c, topLevelFqNames, typeAliases)

        resolveAllHeadersInClasses(c)

        declarationResolver.checkRedeclarationsInPackages(topLevelDescriptorProvider, topLevelFqNames)
        declarationResolver.checkRedeclarations(c)

        overrideResolver.check(c)

        varianceChecker.check(c)

        declarationResolver.resolveAnnotationsOnFiles(c, fileScopeProvider)

        overloadResolver.checkOverloads(c)

        bodyResolver.resolveBodies(c)

        return c
    }

    private fun resolveAllHeadersInClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.allClasses) {
            (classDescriptor as LazyClassDescriptor).resolveMemberHeaders()
        }
    }

    private fun createTypeAliasDescriptors(c: TopDownAnalysisContext, topLevelFqNames: Multimap<FqName, KtElement>, typeAliases: List<KtTypeAlias>) {
        if (!languageFeatureSettings.supportsFeature(LanguageFeature.TypeAliases)) return

        for (typeAlias in typeAliases) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(typeAlias) as TypeAliasDescriptor

            c.typeAliases[typeAlias] = descriptor
            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
            registerTopLevelFqName(topLevelFqNames, typeAlias, descriptor)
        }
    }

    private fun createPropertyDescriptors(c: TopDownAnalysisContext, topLevelFqNames: Multimap<FqName, KtElement>, properties: List<KtProperty>) {
        for (property in properties) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(property) as PropertyDescriptor

            c.properties.put(property, descriptor)
            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
            registerTopLevelFqName(topLevelFqNames, property, descriptor)
        }
    }

    private fun createFunctionDescriptors(c: TopDownAnalysisContext, functions: List<KtNamedFunction>) {
        for (function in functions) {
            val simpleFunctionDescriptor = lazyDeclarationResolver.resolveToDescriptor(function) as SimpleFunctionDescriptor
            c.functions.put(function, simpleFunctionDescriptor)
            ForceResolveUtil.forceResolveAllContents(simpleFunctionDescriptor.annotations)
            for (parameterDescriptor in simpleFunctionDescriptor.valueParameters) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
            }
        }
    }

    private fun registerTopLevelFqName(topLevelFqNames: Multimap<FqName, KtElement>, declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor) {
        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            val fqName = declaration.fqName
            if (fqName != null) {
                topLevelFqNames.put(fqName, declaration)
            }
        }
    }
}


