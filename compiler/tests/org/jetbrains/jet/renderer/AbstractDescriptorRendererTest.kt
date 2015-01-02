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

package org.jetbrains.jet.renderer

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.JetVisitorVoid
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import com.intellij.openapi.editor.impl.DocumentImpl
import org.jetbrains.jet.JetTestUtils
import java.util.ArrayList
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.ConfigurationKind
import com.intellij.testFramework.UsefulTestCase
import java.io.File
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM
import org.jetbrains.jet.context.GlobalContext
import org.jetbrains.kotlin.di.InjectorForLazyResolve
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.jet.lang.resolve.kotlin.JavaDeclarationCheckerProvider
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetClassObject
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetFunctionType
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.types.DynamicTypesSettings

public abstract class AbstractDescriptorRendererTest : KotlinTestWithEnvironment() {
    protected open fun getDescriptor(declaration: JetDeclaration, resolveSession: ResolveSession): DeclarationDescriptor {
        return resolveSession.resolveToDescriptor(declaration)
    }

    public fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val psiFile = JetPsiFactory(getProject()).createFile(fileText)

        val lazyModule = TopDownAnalyzerFacadeForJVM.createSealedJavaModule()
        val globalContext = GlobalContext()

        val resolveSession = InjectorForLazyResolve(
                getProject(), globalContext, lazyModule,
                FileBasedDeclarationProviderFactory(globalContext.storageManager, listOf(psiFile)),
                CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                JavaDeclarationCheckerProvider, DynamicTypesSettings()).getResolveSession()

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

            override fun visitClassObject(classObject: JetClassObject) {
                classObject.acceptChildren(this)
            }

            override fun visitParameter(parameter: JetParameter) {
                val declaringElement = parameter.getParent().getParent()
                when (declaringElement) {
                    is JetFunctionType -> return
                    is JetNamedFunction ->
                        addCorrespondingParameterDescriptor(getDescriptor(declaringElement, resolveSession) as FunctionDescriptor, parameter)
                    is JetClass -> {
                        val jetClass: JetClass = declaringElement
                        val classDescriptor = getDescriptor(jetClass, resolveSession) as ClassDescriptor
                        addCorrespondingParameterDescriptor(classDescriptor.getConstructors().first(), parameter)
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
                    descriptors.addAll(descriptor.getConstructors())
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

        val renderedDescriptors = descriptors.map { DescriptorRenderer.FQ_NAMES_IN_TYPES.render(it) }.joinToString(separator = "\n")

        val document = DocumentImpl(psiFile.getText())
        UsefulTestCase.assertSameLines(JetTestUtils.getLastCommentedLines(document), renderedDescriptors.toString())
    }

    override fun createEnvironment(): JetCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)
    }
}


