/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.asJava;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStrategy;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

class LightClassGenerationStrategy extends GenerationStrategy {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.asJava.LightClassGenerationStrategy");

    private final Stack<StubElement> stubStack;
    private final PsiJavaFileStubImpl answer;
    private final JetLightClass theClass;

    public LightClassGenerationStrategy(JetLightClass aClass, Stack<StubElement> stubStack, PsiJavaFileStubImpl answer) {
        this.theClass = aClass;
        this.stubStack = stubStack;
        this.answer = answer;
    }

    @Override
    public void generateNamespace(
            GenerationState state,
            FqName fqName,
            Collection<JetFile> namespaceFiles,
            CompilationErrorHandler errorHandler
    ) {
        PsiManager manager = PsiManager.getInstance(state.getProject());
        stubStack.push(answer);

        answer.setPsiFactory(new ClsWrapperStubPsiFactory());
        final ClsFileImpl fakeFile =
                new ClsFileImpl((PsiManagerImpl) manager, new ClassFileViewProvider(manager, theClass.getFile().getVirtualFile())) {
                    @NotNull
                    @Override
                    public PsiClassHolderFileStub getStub() {
                        return answer;
                    }
                };

        fakeFile.setPhysical(false);
        answer.setPsi(fakeFile);

        super.generateNamespace(state, fqName, namespaceFiles, errorHandler);
        final StubElement pop = stubStack.pop();
        if (pop != answer) {
            LOG.error("Unbalanced stack operations: " + pop);
        }
    }
}
