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

package org.jetbrains.kotlin.renderer

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.di.InjectorForLazyResolve
import org.jetbrains.kotlin.load.kotlin.KotlinJvmCheckerProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.types.DynamicTypesSettings
import java.io.File
import java.util.ArrayList

public abstract class AbstractDescriptorRendererTest : KotlinTestWithEnvironment() {
    protected open fun getDescriptor(declaration: JetDeclaration, resolveSession: ResolveSession): DeclarationDescriptor {
        return resolveSession.resolveToDescriptor(declaration)
    }

    public fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val psiFile = JetPsiFactory(getProject()).createFile(fileText)

        val lazyModule = TopDownAnalyzerFacadeForJVM.createSealedJavaModule()
        val moduleContext = ModuleContext(lazyModule, getProject())

        val resolveSession = InjectorForLazyResolve(
                moduleContext,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, listOf(psiFile)),
                CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                KotlinJvmCheckerProvider, DynamicTypesSettings()).getResolveSession()

        lazyModule.initialize(resolveSession.getPackageFragmentProvider())

        val descriptors = ArrayList<DeclarationDescriptor>()

        psiFile.accept(object : JetVisitorVoid() {
            override fun visitJetFile(file: JetFile) {
                val fqName = file.getPackageFqName()
                if (!fqName.isRoot()) {
                    val packageDescriptor = lazyModule.getPackage(fqName)
                    descriptors.add(packageDescriptor)
                }
                file.acceptChildren(this)
            }

            override fun visitParameter(parameter: JetParameter) {
                val declaringElement = parameter.getParent().getParent()
                when (declaringElement) {
                    is JetFunctionType -> return
                    is JetNamedFunction ->
                        addCorrespondingParameterDescriptor(getDescriptor(declaringElement, resolveSession) as FunctionDescriptor, parameter)
                    is JetPrimaryConstructor -> {
                        val jetClass: JetClass = declaringElement.getContainingClass()
                        val classDescriptor = getDescriptor(jetClass, resolveSession) as ClassDescriptor
                        addCorrespondingParameterDescriptor(classDescriptor.getUnsubstitutedPrimaryConstructor(), parameter)
                    }
                    else ->  super.visitParameter(parameter)
                }
            }

            override fun visitPropertyAccessor(accessor: JetPropertyAccessor) {
                val parent = accessor.getParent() as JetProperty
                val propertyDescriptor = getDescriptor(parent, resolveSession) as PropertyDescriptor
                if (accessor.isGetter()) {
                    descriptors.add(propertyDescriptor.getGetter())
                }
                else {
                    descriptors.add(propertyDescriptor.getSetter())
                }
                accessor.acceptChildren(this)
            }

            override fun visitAnonymousInitializer(initializer: JetClassInitializer) {
                initializer.acceptChildren(this)
            }

            override fun visitDeclaration(element: JetDeclaration) {
                val descriptor = getDescriptor(element, resolveSession)
                descriptors.add(descriptor)
                if (descriptor is ClassDescriptor) {
                    // if class has primary constructor then we visit it later, otherwise add it artificially
                    if (element !is JetClass || !element.hasExplicitPrimaryConstructor()) {
                        if (descriptor.getUnsubstitutedPrimaryConstructor() != null) {
                            descriptors.add(descriptor.getUnsubstitutedPrimaryConstructor())
                        }
                    }
                }
                element.acceptChildren(this)
            }

            override fun visitJetElement(element: JetElement) {
                element.acceptChildren(this)
            }

            private fun addCorrespondingParameterDescriptor(functionDescriptor: FunctionDescriptor, parameter: JetParameter) {
                for (valueParameterDescriptor in functionDescriptor.getValueParameters()) {
                    if (valueParameterDescriptor.getName() == parameter.getNameAsName()) {
                        descriptors.add(valueParameterDescriptor)
                    }
                }
                parameter.acceptChildren(this)
            }
        })

        val renderer = DescriptorRendererBuilder().setNameShortness(NameShortness.FULLY_QUALIFIED).build()
        val renderedDescriptors = descriptors.map { renderer.render(it) }.joinToString(separator = "\n")

        val document = DocumentImpl(psiFile.getText())
        UsefulTestCase.assertSameLines(JetTestUtils.getLastCommentedLines(document), renderedDescriptors.toString())
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)
    }
}


