/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedLazyResolveStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractLazyResolveDescriptorRendererTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    protected void doTest(@NotNull String testFile) throws IOException {
        JetFile psiFile = JetPsiFactory.createFile(getProject(), FileUtil.loadFile(new File(testFile), true));
        Collection<JetFile> files = Lists.newArrayList(psiFile);

        final ModuleDescriptorImpl lazyModule = AnalyzerFacadeForJVM.createJavaModule("<lazy module>");
        lazyModule.addFragmentProvider(KotlinBuiltIns.getInstance().getBuiltInsModule().getPackageFragmentProvider());
        LockBasedLazyResolveStorageManager storageManager = new LockBasedLazyResolveStorageManager();
        final ResolveSession resolveSession = new ResolveSession(getProject(), storageManager, lazyModule,
                                                                 new FileBasedDeclarationProviderFactory(storageManager, files));

        final List<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();
        psiFile.accept(new JetVisitorVoid() {
            @Override
            public void visitJetFile(@NotNull JetFile file) {
                FqName fqName = file.getNamespaceHeader().getFqName();
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
                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) resolveSession.resolveToDescriptor(jetNamedFunction);
                    addCorrespondingParameterDescriptor(functionDescriptor, parameter);
                }
                else if (declaringElement instanceof JetClass) {
                    // Primary constructor parameter
                    JetClass jetClass = (JetClass) declaringElement;
                    ClassDescriptor classDescriptor = resolveSession.getClassDescriptor(jetClass);
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
            }

            @Override
            public void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor) {
                JetProperty parent = (JetProperty) accessor.getParent();
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) resolveSession.resolveToDescriptor(parent);
                if (accessor.isGetter()) {
                    descriptors.add(propertyDescriptor.getGetter());
                }
                else {
                    descriptors.add(propertyDescriptor.getSetter());
                }
            }

            @Override
            public void visitDeclaration(@NotNull JetDeclaration element) {
                DeclarationDescriptor descriptor = resolveSession.resolveToDescriptor(element);
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
            renderedDescriptors.append(DescriptorRenderer.TEXT.render(descriptor));
        }

        Document document = new DocumentImpl(psiFile.getText());
        assertEquals(JetTestUtils.getLastCommentedLines(document), renderedDescriptors.toString());
    }
}
