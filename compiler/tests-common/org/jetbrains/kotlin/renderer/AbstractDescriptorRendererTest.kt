/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import java.util.*

abstract class AbstractDescriptorRendererTest : KotlinTestWithEnvironment() {
    protected open fun getDescriptor(declaration: KtDeclaration, container: ComponentProvider): DeclarationDescriptor {
        return container.get<ResolveSession>().resolveToDescriptor(declaration)
    }

    protected open val targetEnvironment: TargetEnvironment
        get() = CompilerEnvironment

    fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val psiFile = KtPsiFactory(project).createFile(fileText)

        val context = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, environment.configuration)

        val container = createContainerForLazyResolve(
                context,
                FileBasedDeclarationProviderFactory(context.storageManager, listOf(psiFile)),
                CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                JvmPlatform,
                targetEnvironment,
                LanguageVersionSettingsImpl.DEFAULT
        )

        val resolveSession = container.get<ResolveSession>()

        context.initializeModuleContents(resolveSession.packageFragmentProvider)

        val descriptors = ArrayList<DeclarationDescriptor>()

        psiFile.accept(object : KtVisitorVoid() {
            override fun visitKtFile(file: KtFile) {
                val fqName = file.packageFqName
                if (!fqName.isRoot) {
                    val packageDescriptor = context.module.getPackage(fqName)
                    descriptors.add(packageDescriptor)
                }
                file.acceptChildren(this)
            }

            override fun visitParameter(parameter: KtParameter) {
                val declaringElement = parameter.parent.parent
                when (declaringElement) {
                    is KtFunctionType -> return
                    is KtNamedFunction ->
                        addCorrespondingParameterDescriptor(getDescriptor(declaringElement, container) as FunctionDescriptor, parameter)
                    is KtPrimaryConstructor -> {
                        val ktClassOrObject: KtClassOrObject = declaringElement.getContainingClassOrObject()
                        val classDescriptor = getDescriptor(ktClassOrObject, container) as ClassDescriptor
                        addCorrespondingParameterDescriptor(classDescriptor.unsubstitutedPrimaryConstructor!!, parameter)
                    }
                    else -> super.visitParameter(parameter)
                }
            }

            override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                val property = accessor.property
                val propertyDescriptor = getDescriptor(property, container) as PropertyDescriptor
                if (accessor.isGetter) {
                    descriptors.add(propertyDescriptor.getter!!)
                }
                else {
                    descriptors.add(propertyDescriptor.setter!!)
                }
                accessor.acceptChildren(this)
            }

            override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer) {
                initializer.acceptChildren(this)
            }

            override fun visitDeclaration(element: KtDeclaration) {
                val descriptor = getDescriptor(element, container)
                descriptors.add(descriptor)
                if (descriptor is ClassDescriptor) {
                    // if class has primary constructor then we visit it later, otherwise add it artificially
                    if (element !is KtClassOrObject || !element.hasExplicitPrimaryConstructor()) {
                        if (descriptor.unsubstitutedPrimaryConstructor != null) {
                            descriptors.add(descriptor.unsubstitutedPrimaryConstructor!!)
                        }
                    }
                }
                element.acceptChildren(this)
            }

            override fun visitKtElement(element: KtElement) {
                element.acceptChildren(this)
            }

            private fun addCorrespondingParameterDescriptor(functionDescriptor: FunctionDescriptor, parameter: KtParameter) {
                for (valueParameterDescriptor in functionDescriptor.valueParameters) {
                    if (valueParameterDescriptor.name == parameter.nameAsName) {
                        descriptors.add(valueParameterDescriptor)
                    }
                }
                parameter.acceptChildren(this)
            }
        })

        val renderer = DescriptorRenderer.withOptions {
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            modifiers = DescriptorRendererModifier.ALL
            includeAnnotationArguments = true
        }
        val renderedDescriptors = descriptors.map { renderer.render(it) }.joinToString(separator = "\n")

        val document = DocumentImpl(psiFile.text)
        KtUsefulTestCase.assertSameLines(KotlinTestUtils.getLastCommentedLines(document), renderedDescriptors.toString())
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)
    }
}


