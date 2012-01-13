/*
 * @author max
 */
package org.jetbrains.jet.asJava;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
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
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.Collections;
import java.util.List;

public class JetLightClass extends AbstractLightClass implements JetJavaMirrorMarker {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.asJava.JetLightClass");
    private final static Key<CachedValue<PsiJavaFileStub>> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    private final JetFile file;
    private final String qualifiedName;
    private PsiClass delegate;

    public JetLightClass(PsiManager manager, JetFile file, String qualifiedName) {
        super(manager, JetLanguage.INSTANCE);
        this.file = file;
        this.qualifiedName = qualifiedName;
    }

    @Override
    public String getName() {
        int idx = qualifiedName.lastIndexOf('.');
        return idx > 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
    }

    @Override
    public PsiElement copy() {
        return new JetLightClass(getManager(), file, qualifiedName);
    }

    @Override
    public PsiFile getContainingFile() {
        return file;
    }

    @Override
    public PsiClass getDelegate() {
        if (delegate == null) {
            delegate = findClass(qualifiedName, getStub());
            if (delegate == null) {
                delegate = findClass(qualifiedName, getStub());
            }
        }
        return delegate;
    }

    private static PsiClass findClass(String fqn, StubElement<?> stub) {
        if (stub instanceof PsiClassStub && Comparing.equal(fqn, ((PsiClassStub) stub).getQualifiedName())) {
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
        return qualifiedName;
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
        final PsiJavaFileStubImpl answer = new PsiJavaFileStubImpl(JetPsiUtil.getFQName(file), true);
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


        List<JetFile> files = Collections.singletonList(file);
        final BindingContext context = AnalyzerFacade.shallowAnalyzeFiles(files);
        state.compileCorrectFiles(context, files);
        state.getFactory().files();

        return answer;
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof PsiClass && Comparing.equal(((PsiClass) another).getQualifiedName(), getQualifiedName());
    }
}
