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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.resolveUtil.*
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker

import javax.inject.Inject
import java.util.ArrayList

import org.jetbrains.kotlin.diagnostics.Errors.*

public class LazyTopDownAnalyzer {
    private var trace: BindingTrace? = null

    private var declarationResolver: DeclarationResolver? = null

    private var overrideResolver: OverrideResolver? = null

    private var overloadResolver: OverloadResolver? = null

    private var varianceChecker: VarianceChecker? = null

    private var moduleDescriptor: ModuleDescriptor? = null

    private var lazyDeclarationResolver: LazyDeclarationResolver? = null

    private var bodyResolver: BodyResolver? = null

    private var topLevelDescriptorProvider: TopLevelDescriptorProvider? = null

    private var fileScopeProvider: FileScopeProvider? = null

    private var declarationScopeProvider: DeclarationScopeProvider? = null

    Inject
    public fun setLazyDeclarationResolver(lazyDeclarationResolver: LazyDeclarationResolver) {
        this.lazyDeclarationResolver = lazyDeclarationResolver
    }

    Inject
    public fun setTopLevelDescriptorProvider(topLevelDescriptorProvider: TopLevelDescriptorProvider) {
        this.topLevelDescriptorProvider = topLevelDescriptorProvider
    }

    Inject
    public fun setFileScopeProvider(fileScopeProvider: FileScopeProvider) {
        this.fileScopeProvider = fileScopeProvider
    }

    Inject
    public fun setDeclarationScopeProvider(declarationScopeProvider: DeclarationScopeProviderImpl) {
        this.declarationScopeProvider = declarationScopeProvider
    }

    Inject
    public fun setTrace(trace: BindingTrace) {
        this.trace = trace
    }

    Inject
    public fun setDeclarationResolver(declarationResolver: DeclarationResolver) {
        this.declarationResolver = declarationResolver
    }

    Inject
    public fun setOverrideResolver(overrideResolver: OverrideResolver) {
        this.overrideResolver = overrideResolver
    }

    Inject
    public fun setVarianceChecker(varianceChecker: VarianceChecker) {
        this.varianceChecker = varianceChecker
    }

    Inject
    public fun setOverloadResolver(overloadResolver: OverloadResolver) {
        this.overloadResolver = overloadResolver
    }

    Inject
    public fun setModuleDescriptor(moduleDescriptor: ModuleDescriptor) {
        this.moduleDescriptor = moduleDescriptor
    }

    Inject
    public fun setBodyResolver(bodyResolver: BodyResolver) {
        this.bodyResolver = bodyResolver
    }

    public fun analyzeDeclarations(topDownAnalysisMode: TopDownAnalysisMode, declarations: Collection<PsiElement>, outerDataFlowInfo: DataFlowInfo): TopDownAnalysisContext {
        val c = TopDownAnalysisContext(topDownAnalysisMode, outerDataFlowInfo)

        val topLevelFqNames = HashMultimap.create<FqName, JetElement>()

        val properties = ArrayList<JetProperty>()
        val functions = ArrayList<JetNamedFunction>()

        // fill in the context
        for (declaration in declarations) {
            declaration.accept(object : JetVisitorVoid() {
                private fun registerDeclarations(declarations: List<JetDeclaration>) {
                    for (jetDeclaration in declarations) {
                        jetDeclaration.accept(this)
                    }
                }

                override fun visitDeclaration(dcl: JetDeclaration) {
                    throw IllegalArgumentException("Unsupported declaration: " + dcl + " " + dcl.getText())
                }

                override fun visitJetFile(file: JetFile) {
                    if (file.isScript()) {
                        val script = file.getScript()
                        assert(script != null)

                        DescriptorResolver.registerFileInPackage(trace, file)
                        c.getScripts().put(script, topLevelDescriptorProvider!!.getScriptDescriptor(script))
                    }
                    else {
                        val packageDirective = file.getPackageDirective()
                        assert(packageDirective != null) { "No package in a non-script file: " + file }

                        c.addFile(file)

                        packageDirective!!.accept(this)
                        DescriptorResolver.registerFileInPackage(trace, file)

                        registerDeclarations(file.getDeclarations())

                        topLevelFqNames.put(file.getPackageFqName(), packageDirective)
                    }
                }

                override fun visitPackageDirective(directive: JetPackageDirective) {
                    DescriptorResolver.resolvePackageHeader(directive, moduleDescriptor, trace)
                }

                override fun visitImportDirective(importDirective: JetImportDirective) {
                    val fileScope = fileScopeProvider!!.getFileScope(importDirective.getContainingJetFile()) as LazyFileScope
                    fileScope.forceResolveImport(importDirective)
                }

                private fun visitClassOrObject(classOrObject: JetClassOrObject) {
                    val descriptor = lazyDeclarationResolver!!.getClassDescriptor(classOrObject) as ClassDescriptorWithResolutionScopes

                    c.getDeclaredClasses().put(classOrObject, descriptor)
                    registerDeclarations(classOrObject.getDeclarations())
                    registerTopLevelFqName(topLevelFqNames, classOrObject, descriptor)

                    checkClassOrObjectDeclarations(classOrObject, descriptor)
                }

                private fun checkClassOrObjectDeclarations(classOrObject: JetClassOrObject, classDescriptor: ClassDescriptor) {
                    var companionObjectAlreadyFound = false
                    for (jetDeclaration in classOrObject.getDeclarations()) {
                        if (jetDeclaration is JetObjectDeclaration && jetDeclaration.isCompanion()) {
                            if (companionObjectAlreadyFound) {
                                trace!!.report(MANY_COMPANION_OBJECTS.on(jetDeclaration))
                            }
                            companionObjectAlreadyFound = true
                        }
                        else if (jetDeclaration is JetSecondaryConstructor) {
                            if (DescriptorUtils.isSingletonOrAnonymousObject(classDescriptor)) {
                                trace!!.report(SECONDARY_CONSTRUCTOR_IN_OBJECT.on(jetDeclaration))
                            }
                            else if (classDescriptor.getKind() == ClassKind.INTERFACE) {
                                trace!!.report(CONSTRUCTOR_IN_TRAIT.on(jetDeclaration))
                            }
                        }
                    }
                }

                override fun visitClass(klass: JetClass) {
                    visitClassOrObject(klass)
                    registerPrimaryConstructorParameters(klass)
                }

                private fun registerPrimaryConstructorParameters(klass: JetClass) {
                    for (jetParameter in klass.getPrimaryConstructorParameters()) {
                        if (jetParameter.hasValOrVarNode()) {
                            c.getPrimaryConstructorParameterProperties().put(jetParameter, lazyDeclarationResolver!!.resolveToDescriptor(jetParameter) as PropertyDescriptor)
                        }
                    }
                }

                override fun visitSecondaryConstructor(constructor: JetSecondaryConstructor) {
                    val classDescriptor = lazyDeclarationResolver!!.resolveToDescriptor(constructor.getClassOrObject()) as ClassDescriptor
                    if (!DescriptorUtils.canHaveSecondaryConstructors(classDescriptor)) {
                        return
                    }
                    c.getSecondaryConstructors().put(constructor, lazyDeclarationResolver!!.resolveToDescriptor(constructor) as ConstructorDescriptor)
                    registerScope(c, constructor)
                }

                override fun visitEnumEntry(enumEntry: JetEnumEntry) {
                    visitClassOrObject(enumEntry)
                }

                override fun visitObjectDeclaration(declaration: JetObjectDeclaration) {
                    visitClassOrObject(declaration)
                }

                override fun visitAnonymousInitializer(initializer: JetClassInitializer) {
                    registerScope(c, initializer)
                    val classOrObject = PsiTreeUtil.getParentOfType<JetClassOrObject>(initializer, javaClass<JetClassOrObject>())
                    c.getAnonymousInitializers().put(initializer, lazyDeclarationResolver!!.resolveToDescriptor(classOrObject) as ClassDescriptorWithResolutionScopes)
                }

                override fun visitTypedef(typedef: JetTypedef) {
                    trace!!.report(UNSUPPORTED.on(typedef, "Typedefs are not supported"))
                }

                override fun visitMultiDeclaration(multiDeclaration: JetMultiDeclaration) {
                    // Ignore: multi-declarations are only allowed locally
                }

                override fun visitNamedFunction(function: JetNamedFunction) {
                    functions.add(function)
                }

                override fun visitProperty(property: JetProperty) {
                    properties.add(property)
                }
            })
        }

        createFunctionDescriptors(c, functions)

        createPropertyDescriptors(c, topLevelFqNames, properties)

        resolveAllHeadersInClasses(c)

        declarationResolver!!.checkRedeclarationsInPackages(topLevelDescriptorProvider, topLevelFqNames)
        declarationResolver!!.checkRedeclarations(c)

        checkTraitRequirements(c.getDeclaredClasses(), trace)

        overrideResolver!!.check(c)

        varianceChecker!!.check(c)

        declarationResolver!!.resolveAnnotationsOnFiles(c, fileScopeProvider)

        overloadResolver!!.process(c)

        bodyResolver!!.resolveBodies(c)

        return c
    }

    private fun resolveAllHeadersInClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.getAllClasses()) {
            (classDescriptor as LazyClassDescriptor).resolveMemberHeaders()
        }
    }

    private fun createPropertyDescriptors(c: TopDownAnalysisContext, topLevelFqNames: Multimap<FqName, JetElement>, properties: List<JetProperty>) {
        for (property in properties) {
            val descriptor = lazyDeclarationResolver!!.resolveToDescriptor(property) as PropertyDescriptor

            c.getProperties().put(property, descriptor)
            registerTopLevelFqName(topLevelFqNames, property, descriptor)

            registerScope(c, property)
            registerScope(c, property.getGetter())
            registerScope(c, property.getSetter())
        }
    }

    private fun createFunctionDescriptors(c: TopDownAnalysisContext, functions: List<JetNamedFunction>) {
        for (function in functions) {
            c.getFunctions().put(function, lazyDeclarationResolver!!.resolveToDescriptor(function) as SimpleFunctionDescriptor)
            registerScope(c, function)
        }
    }

    private fun registerScope(c: TopDownAnalysisContext, declaration: JetDeclaration?) {
        if (declaration == null) return
        c.registerDeclaringScope(declaration, declarationScopeProvider!!.getResolutionScopeForDeclaration(declaration))
    }

    private fun registerTopLevelFqName(topLevelFqNames: Multimap<FqName, JetElement>, declaration: JetNamedDeclaration, descriptor: DeclarationDescriptor) {
        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            val fqName = declaration.getFqName()
            if (fqName != null) {
                topLevelFqNames.put(fqName, declaration)
            }
        }
    }
}


