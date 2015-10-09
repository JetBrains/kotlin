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
import org.jetbrains.kotlin.diagnostics.Errors.CONSTRUCTOR_IN_OBJECT
import org.jetbrains.kotlin.diagnostics.Errors.CONSTRUCTOR_IN_INTERFACE
import org.jetbrains.kotlin.diagnostics.Errors.MANY_COMPANION_OBJECTS
import org.jetbrains.kotlin.diagnostics.Errors.UNSUPPORTED
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker
import java.util.ArrayList

public class LazyTopDownAnalyzer(
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
        private val qualifiedExpressionResolver: QualifiedExpressionResolver
) {
    public fun analyzeDeclarations(topDownAnalysisMode: TopDownAnalysisMode, declarations: Collection<PsiElement>, outerDataFlowInfo: DataFlowInfo): TopDownAnalysisContext {
        val c = TopDownAnalysisContext(topDownAnalysisMode, outerDataFlowInfo, declarationScopeProvider)

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
                        val script = file.getScript() ?: throw AssertionError("getScript() is null for file: $file")

                        DescriptorResolver.registerFileInPackage(trace, file)
                        c.getScripts().put(script, topLevelDescriptorProvider.getScriptDescriptor(script))
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
                    qualifiedExpressionResolver.resolvePackageHeader(directive, moduleDescriptor, trace)
                }

                override fun visitImportDirective(importDirective: JetImportDirective) {
                    val fileScope = fileScopeProvider.getFileScope(importDirective.getContainingJetFile())
                    fileScope.forceResolveImport(importDirective)
                }

                override fun visitClassOrObject(classOrObject: JetClassOrObject) {
                    val location = if (classOrObject.isTopLevel()) KotlinLookupLocation(classOrObject) else NoLookupLocation.WHEN_RESOLVE_DECLARATION
                    val descriptor = lazyDeclarationResolver.getClassDescriptor(classOrObject, location) as ClassDescriptorWithResolutionScopes

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
                                trace.report(MANY_COMPANION_OBJECTS.on(jetDeclaration))
                            }
                            companionObjectAlreadyFound = true
                        }
                        else if (jetDeclaration is JetSecondaryConstructor) {
                            if (DescriptorUtils.isSingletonOrAnonymousObject(classDescriptor)) {
                                trace.report(CONSTRUCTOR_IN_OBJECT.on(jetDeclaration))
                            }
                            else if (classDescriptor.getKind() == ClassKind.INTERFACE) {
                                trace.report(CONSTRUCTOR_IN_INTERFACE.on(jetDeclaration))
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
                        if (jetParameter.hasValOrVar()) {
                            c.getPrimaryConstructorParameterProperties().put(jetParameter, lazyDeclarationResolver.resolveToDescriptor(jetParameter) as PropertyDescriptor)
                        }
                    }
                }

                override fun visitSecondaryConstructor(constructor: JetSecondaryConstructor) {
                    c.getSecondaryConstructors().put(constructor, lazyDeclarationResolver.resolveToDescriptor(constructor) as ConstructorDescriptor)
                }

                override fun visitEnumEntry(enumEntry: JetEnumEntry) {
                    visitClassOrObject(enumEntry)
                }

                override fun visitObjectDeclaration(declaration: JetObjectDeclaration) {
                    visitClassOrObject(declaration)
                }

                override fun visitAnonymousInitializer(initializer: JetClassInitializer) {
                    val classOrObject = PsiTreeUtil.getParentOfType<JetClassOrObject>(initializer, javaClass<JetClassOrObject>())!!
                    c.getAnonymousInitializers().put(initializer, lazyDeclarationResolver.resolveToDescriptor(classOrObject) as ClassDescriptorWithResolutionScopes)
                }

                override fun visitTypedef(typedef: JetTypedef) {
                    trace.report(UNSUPPORTED.on(typedef, "Typedefs are not supported"))
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

        declarationResolver.checkRedeclarationsInPackages(topLevelDescriptorProvider, topLevelFqNames)
        declarationResolver.checkRedeclarations(c)

        overrideResolver.check(c)

        varianceChecker.check(c)

        declarationResolver.resolveAnnotationsOnFiles(c, fileScopeProvider)

        overloadResolver.process(c)

        bodyResolver.resolveBodies(c)

        return c
    }

    private fun resolveAllHeadersInClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.getAllClasses()) {
            (classDescriptor as LazyClassDescriptor).resolveMemberHeaders()
        }
    }

    private fun createPropertyDescriptors(c: TopDownAnalysisContext, topLevelFqNames: Multimap<FqName, JetElement>, properties: List<JetProperty>) {
        for (property in properties) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(property) as PropertyDescriptor

            c.getProperties().put(property, descriptor)
            ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations())
            registerTopLevelFqName(topLevelFqNames, property, descriptor)
        }
    }

    private fun createFunctionDescriptors(c: TopDownAnalysisContext, functions: List<JetNamedFunction>) {
        for (function in functions) {
            val simpleFunctionDescriptor = lazyDeclarationResolver.resolveToDescriptor(function) as SimpleFunctionDescriptor
            c.getFunctions().put(function, simpleFunctionDescriptor)
            ForceResolveUtil.forceResolveAllContents(simpleFunctionDescriptor.getAnnotations())
            for (parameterDescriptor in simpleFunctionDescriptor.getValueParameters()) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.getAnnotations())
            }
        }
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


