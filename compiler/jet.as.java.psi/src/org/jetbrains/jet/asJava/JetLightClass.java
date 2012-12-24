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

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.*;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStrategy;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetLanguage;

import javax.swing.*;
import java.util.Collections;

public class JetLightClass extends AbstractLightClass implements JetJavaMirrorMarker {
    static class JetBadWrapperException extends RuntimeException {
        private final String context;

        public JetBadWrapperException(@Nullable Exception exc, @NotNull JetNamedDeclaration originalElement, PsiElement wrappedElement) {
            super("Error while wrapping function " + originalElement.getName(), exc);
            context = String.format("=== In file ===\n" +
                                    "%s\n" +
                                    "===On element===\n" +
                                    "%s\n" +
                                    "===WrappedElement===\n" +
                                    "%s\n",
                                    originalElement.getContainingFile().getText(),
                                    originalElement.getText(),
                                    wrappedElement.toString());
        }

        @Override
        public String toString() {
            return super.toString() + "\nContext:\n" + context;
        }
    }

    private final static Key<CachedValue<PsiJavaFileStub>> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    private final JetFile file;
    private final FqName qualifiedName;
    private PsiClass delegate;

    private JetLightClass(PsiManager manager, JetFile file, FqName qualifiedName) {
        super(manager, JetLanguage.INSTANCE);
        this.file = file;
        this.qualifiedName = qualifiedName;
    }

    @Nullable
    public static JetLightClass create(@NotNull PsiManager manager, @NotNull JetFile file, @NotNull FqName qualifiedName) {
        if (LightClassUtil.belongsToKotlinBuiltIns(file)) {
            return null;
        }
        return new JetLightClass(manager, file, qualifiedName);
    }

    @Override
    public String getName() {
        return qualifiedName.shortName().getName();
    }

    @NotNull
    @Override
    public PsiElement copy() {
        JetLightClass result = create(getManager(), file, qualifiedName);
        assert result != null;
        return result;
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
        if (stub instanceof PsiClassStub && Comparing.equal(fqn.getFqName(), ((PsiClassStub)stub).getQualifiedName())) {
            return (PsiClass)stub.getPsi();
        }

        if (stub instanceof PsiClassStub || stub instanceof PsiFileStub) {
            for (StubElement child : stub.getChildrenStubs()) {
                PsiClass answer = findClass(fqn, child);
                if (answer != null) return answer;
            }
        }

        return null;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName.getFqName();
    }

    public FqName getFqName() {
        return qualifiedName;
    }

    private PsiJavaFileStub getStub() {
        CachedValue<PsiJavaFileStub> answer = file.getUserData(JAVA_API_STUB);
        if (answer == null) {
            answer = CachedValuesManager.getManager(getProject()).createCachedValue(new CachedValueProvider<PsiJavaFileStub>() {
                @Override
                public Result<PsiJavaFileStub> compute() {
                    //System.out.println("Calculating Java stub for " + file.getName() + ", OOCB modcount " + PsiModificationTracker.SERVICE.getInstance(getProject()).getOutOfCodeBlockModificationCount());
                    return Result.create(calcStub(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                }
            }, false);
            file.putUserData(JAVA_API_STUB, answer);
        }

        return answer.getValue();
    }

    private PsiJavaFileStub calcStub() {
        if (LightClassUtil.belongsToKotlinBuiltIns(file)) {
            // We may not fail later due to some luck, but generating JetLightClasses for built-ins is a bad idea anyways
            // If it fails later, there will be an exception logged
            LightClassUtil.logErrorWithOSInfo(null, qualifiedName, file.getVirtualFile());
        }

        final PsiJavaFileStubImpl answer = new PsiJavaFileStubImpl(JetPsiUtil.getFQName(file).getFqName(), true);
        final Project project = getProject();

        final Stack<StubElement> stubStack = new Stack<StubElement>();

        final ClassBuilderFactory builderFactory = new ClassBuilderFactory() {
            @NotNull
            @Override
            public ClassBuilderMode getClassBuilderMode() {
                return ClassBuilderMode.SIGNATURES;
            }

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

        // The context must reflect _all files in the module_. not only the current file
        // Otherwise, the analyzer gets confused and can't, for example, tell which files come as sources and which
        // must be loaded from .class files
        LightClassConstructionContext context = LightClassGenerationSupport.getInstance(project).analyzeRelevantCode(Collections.singletonList(file));

        Throwable error = context.getError();
        if (error != null) {
            throw new IllegalStateException("failed to analyze: " + error, error);
        }

        try {
            GenerationState state = new GenerationState(project, builderFactory, context.getBindingContext(), Collections.singletonList(file));
            VirtualFile virtualFile = file.getVirtualFile();
            assert virtualFile != null;

            GenerationStrategy strategy = new LightClassGenerationStrategy(virtualFile, stubStack, answer);
            KotlinCodegenFacade.compileCorrectFiles(state, strategy, CompilationErrorHandler.THROW_EXCEPTION);

            state.getFactory().files();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            LightClassUtil.logErrorWithOSInfo(e, qualifiedName, file.getVirtualFile());
            throw e;
        }

        return answer;
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof PsiClass && Comparing.equal(((PsiClass)another).getQualifiedName(), getQualifiedName());
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public Icon getElementIcon(final int flags) {
        return PsiClassImplUtil.getClassIcon(flags, this);
    }

    @Override
    public String toString() {
        try {
            return JetLightClass.class.getSimpleName() + ":" + getQualifiedName();
        }
        catch (Throwable e) {
            return JetLightClass.class.getSimpleName() + ":" + e.toString();
        }
    }

    @Override
    public int hashCode() {
        int result = getManager().hashCode();
        result = 31 * result + file.hashCode();
        result = 31 * result + qualifiedName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        JetLightClass lightClass = (JetLightClass) obj;

        if (getManager() != lightClass.getManager()) return false;
        if (!file.equals(lightClass.file)) return false;
        if (!qualifiedName.equals(lightClass.qualifiedName)) return false;

        return true;
    }

    @Nullable
    public static JetLightClass wrapDelegate(@Nullable JetClass jetClass) {
        if (jetClass == null) {
            return null;
        }

        return wrapDelegate(jetClass, PsiCodegenPredictor.getPredefinedJvmClassName(jetClass));
    }

    @Nullable
    private static JetLightClass wrapDelegate(@NotNull JetDeclaration declaration, @Nullable JvmClassName jvmClassName) {
        if (jvmClassName == null) {
            return null;
        }

        return create(declaration.getManager(), (JetFile) declaration.getContainingFile(), jvmClassName.getFqName());
    }

    @Nullable
    public static PsiMethod wrapMethod(@NotNull JetNamedFunction function) {
        //noinspection unchecked
        if (PsiTreeUtil.getParentOfType(function, JetFunction.class, JetProperty.class) != null) {
            // Don't genClassOrObject method wrappers for internal declarations. Their classes are not generated during calcStub
            // with ClassBuilderMode.SIGNATURES mode, and this produces "Class not found exception" in getDelegate()
            return null;
        }

        JetLightClass wrapper = wrapDelegate(function, PsiCodegenPredictor.getPredefinedJvmClassNameForFun(function));
        if (wrapper == null) {
            return null;
        }

        for (PsiMethod method : wrapper.getMethods()) {
            try {
                if (method instanceof PsiCompiledElement && ((PsiCompiledElement) method).getMirror() == function) {
                    return method;
                }
            }
            catch (Exception exception) {
                throw new JetBadWrapperException(exception, function, method);
            }
        }

        return null;
    }

    public JetFile getFile() {
        return file;
    }
}
