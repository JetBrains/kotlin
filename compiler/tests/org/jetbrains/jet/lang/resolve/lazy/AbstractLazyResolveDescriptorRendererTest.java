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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Lists;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForLazyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public abstract class AbstractLazyResolveDescriptorRendererTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    protected DeclarationDescriptor getDescriptor(JetDeclaration declaration, ResolveSession resolveSession) {
        return resolveSession.resolveToDescriptor(declaration);
    }

    protected void doTest(@NotNull String testFile) throws IOException {
        String fileText = FileUtil.loadFile(new File(testFile), true);

        JetFile psiFile = JetPsiFactory(getProject()).createFile(fileText);
        Collection<JetFile> files = Lists.newArrayList(psiFile);

        final ModuleDescriptorImpl lazyModule = AnalyzerFacadeForJVM.createJavaModule("<lazy module>");
        lazyModule.addDependencyOnModule(lazyModule);
        lazyModule.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        lazyModule.seal();
        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        final ResolveSession resolveSession = new InjectorForLazyResolve(
                getProject(), globalContext, lazyModule,
                new FileBasedDeclarationProviderFactory(globalContext.getStorageManager(), files),
                new BindingTraceContext()).getResolveSession();
        lazyModule.initialize(resolveSession.getPackageFragmentProvider());

        final List<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();
        psiFile.accept(new JetVisitorVoid() {
            @Override
            public void visitJetFile(@NotNull JetFile file) {
                FqName fqName = file.getPackageFqName();
                if (!fqName.isRoot()) {
                    PackageViewDescriptor packageDescriptor = lazyModule.getPackage(fqName);
                    descriptors.add(packageDescriptor);
                }
                file.acceptChildren(this);
            }

            @Override
            public void visitClassObject(@NotNull JetClassObject classObject) {
                classObject.acceptChildren(this);
            }

            @Override
            public void visitParameter(@NotNull JetParameter parameter) {
                PsiElement declaringElement = parameter.getParent().getParent();
                if (declaringElement instanceof JetFunctionType) {
                    return;
                }
                if (declaringElement instanceof JetNamedFunction) {
                    JetNamedFunction jetNamedFunction = (JetNamedFunction) declaringElement;
                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) getDescriptor(jetNamedFunction, resolveSession);
                    addCorrespondingParameterDescriptor(functionDescriptor, parameter);
                }
                else if (declaringElement instanceof JetClass) {
                    // Primary constructor parameter
                    JetClass jetClass = (JetClass) declaringElement;
                    ClassDescriptor classDescriptor = (ClassDescriptor) getDescriptor(jetClass, resolveSession);
                    addCorrespondingParameterDescriptor(classDescriptor.getConstructors().iterator().next(), parameter);
                }
                else {
                    super.visitParameter(parameter);
                }
            }

            private void addCorrespondingParameterDescriptor(FunctionDescriptor functionDescriptor, JetParameter parameter) {
                for (ValueParameterDescriptor valueParameterDescriptor : functionDescriptor.getValueParameters()) {
                    if (valueParameterDescriptor.getName().equals(parameter.getNameAsName())) {
                        descriptors.add(valueParameterDescriptor);
                    }
                }
                parameter.acceptChildren(this);
            }

            @Override
            public void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor) {
                JetProperty parent = (JetProperty) accessor.getParent();
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) getDescriptor(parent, resolveSession);
                if (accessor.isGetter()) {
                    descriptors.add(propertyDescriptor.getGetter());
                }
                else {
                    descriptors.add(propertyDescriptor.getSetter());
                }
                accessor.acceptChildren(this);
            }

            @Override
            public void visitAnonymousInitializer(@NotNull JetClassInitializer initializer) {
                initializer.acceptChildren(this);
            }

            @Override
            public void visitDeclaration(@NotNull JetDeclaration element) {
                DeclarationDescriptor descriptor = getDescriptor(element, resolveSession);
                descriptors.add(descriptor);
                if (descriptor instanceof ClassDescriptor) {
                    descriptors.addAll(((ClassDescriptor) descriptor).getConstructors());
                }
                element.acceptChildren(this);
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                element.acceptChildren(this);
            }
        });

        StringBuilder renderedDescriptors = new StringBuilder();
        for (DeclarationDescriptor descriptor : descriptors) {
            if (renderedDescriptors.length() != 0) {
                renderedDescriptors.append("\n");
            }
            renderedDescriptors.append(DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor));
        }

        Document document = new DocumentImpl(psiFile.getText());
        assertEquals(JetTestUtils.getLastCommentedLines(document), renderedDescriptors.toString());
    }
}
