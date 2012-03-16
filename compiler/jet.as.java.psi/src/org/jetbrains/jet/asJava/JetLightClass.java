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

/*
 * @author max
 */
package org.jetbrains.jet.asJava;

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import javax.swing.*;
import java.util.Collections;

public class JetLightClass extends AbstractLightClass implements JetJavaMirrorMarker {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.asJava.JetLightClass");
    private final static Key<CachedValue<PsiJavaFileStub>> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    private final JetFile file;
    private final FqName qualifiedName;
    private PsiClass delegate;

    public JetLightClass(PsiManager manager, JetFile file, FqName qualifiedName) {
        super(manager, JetLanguage.INSTANCE);
        this.file = file;
        this.qualifiedName = qualifiedName;
    }

    @Override
    public String getName() {
        return QualifiedNamesUtil.fqnToShortName(qualifiedName);
    }

    @Override
    public PsiElement copy() {
        return new JetLightClass(getManager(), file, qualifiedName);
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        if (delegate == null) {
            delegate = findClass(qualifiedName, getStub());
            if (delegate == null) {
                throw new IllegalStateException("Class not found for qualified name: " + qualifiedName);
            }
        }
        return delegate;
    }

    @Override
    public PsiFile getContainingFile() {
        return file;
    }

    private static PsiClass findClass(FqName fqn, StubElement<?> stub) {
        if (stub instanceof PsiClassStub && Comparing.equal(fqn.getFqName(), ((PsiClassStub) stub).getQualifiedName())) {
            return (PsiClass) stub.getPsi();
        }

        for (StubElement child : stub.getChildrenStubs()) {
            PsiClass answer = findClass(fqn, child);
            if (answer != null) return answer;
        }

        return null;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName.getFqName();
    }

    private PsiJavaFileStub getStub() {
        CachedValue<PsiJavaFileStub> answer = file.getUserData(JAVA_API_STUB);
        if (answer == null) {
            answer = CachedValuesManager.getManager(getProject()).createCachedValue(new CachedValueProvider<PsiJavaFileStub>() {
                @Override
                public Result<PsiJavaFileStub> compute() {
                    return Result.create(calcStub(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                }
            }, false);
            file.putUserData(JAVA_API_STUB, answer);
        }
        
        return answer.getValue();
    }
    
    private PsiJavaFileStub calcStub() {
        final PsiJavaFileStubImpl answer = new PsiJavaFileStubImpl(JetPsiUtil.getFQName(file).getFqName(), true);
        final Project project = getProject();
        
        final Stack<StubElement> stubStack = new Stack<StubElement>();

        final ClassBuilderFactory builderFactory = new ClassBuilderFactory() {
            @Override
            public ClassBuilder newClassBuilder() {
                return new StubClassBuilder(stubStack);
            }

            @Override
            public String asText(ClassBuilder builder) {
                throw new UnsupportedOperationException("asText is not implemented"); // TODO
            }

            @Override
            public byte[] asBytes(ClassBuilder builder) {
                throw new UnsupportedOperationException("asBytes is not implemented"); // TODO
            }
        };

        final GenerationState state = new GenerationState(project, builderFactory) {
            @Override
            protected void generateNamespace(JetFile namespace) {
                PsiManager manager = PsiManager.getInstance(project);
                stubStack.push(answer);

                answer.setPsiFactory(new ClsWrapperStubPsiFactory());
                final ClsFileImpl fakeFile = new ClsFileImpl((PsiManagerImpl) manager, new ClassFileViewProvider(manager, file.getVirtualFile())) {
                    @NotNull
                    @Override
                    public PsiClassHolderFileStub getStub() {
                        return answer;
                    }
                };

                fakeFile.setPhysical(false);
                answer.setPsi(fakeFile);

                try {
                    super.generateNamespace(namespace);
                }
                finally {
                    final StubElement pop = stubStack.pop();
                    if (pop != answer) {
                        LOG.error("Unbalanced stack operations: " + pop);
                    }
                }
            }
        };


        // The context must reflect _all files in the module_. not only the current file
        // Otherwise, the analyzer gets confused and can't, for example, tell which files come as sources and which
        // must be loaded from .class files
        BindingContext context = AnalyzerFacadeForJVM.shallowAnalyzeFiles(WholeProjectAnalyzerFacade.WHOLE_PROJECT_DECLARATION_PROVIDER.fun(file));

        state.compileCorrectFiles(context, Collections.singletonList(file), CompilationErrorHandler.THROW_EXCEPTION, true);
        state.getFactory().files();

        return answer;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof PsiClass && Comparing.equal(((PsiClass) another).getQualifiedName(), getQualifiedName());
    }

    @Override
    public Icon getElementIcon(final int flags) {
        return PsiClassImplUtil.getClassIcon(flags, this);
    }

    @Override
    public String toString() {
        try {
            return JetLightClass.class.getSimpleName() + ":" + getQualifiedName();
        } catch (Throwable e) {
            return JetLightClass.class.getSimpleName() + ":" + e.toString();
        }
    }
}
