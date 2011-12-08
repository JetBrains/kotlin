/*
 * @author max
 */
package org.jetbrains.jet.plugin.java;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.CodegenUtil;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.Collections;
import java.util.List;

public class JetLightClass extends AbstractLightClass implements JetJavaMirrorMarker {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.java.JetLightClass");
    private final static Key<PsiJavaFileStub> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    private final JetFile file;
    private final String className;
    private PsiClass delegate;

    public JetLightClass(PsiManager manager, JetFile file, String className) {
        super(manager, JetLanguage.INSTANCE);
        this.file = file;
        this.className = className;
    }

    @Override
    public String getName() {
        return className;
    }

    @Override
    public PsiElement copy() {
        return new JetLightClass(getManager(), file, className);
    }

    @Override
    public PsiClass getDelegate() {
        if (delegate == null) {
            delegate = findClass(className, getStub());
        }
        return delegate;
    }

    private static PsiClass findClass(String name, StubElement<?> stub) {
        if (stub instanceof PsiClassStub && name.equals(((PsiClassStub) stub).getName())) {
            return (PsiClass) stub.getPsi();
        }

        for (StubElement child : stub.getChildrenStubs()) {
            PsiClass answer = findClass(name, child);
            if (answer != null) return answer;
        }

        return null;
    }
    
    
    private PsiJavaFileStub getStub() {
        PsiJavaFileStub answer = file.getUserData(JAVA_API_STUB);
        if (answer == null) {
            answer = calcStub();
            file.putUserData(JAVA_API_STUB, answer);
        }
        
        return answer;
    }
    
    private PsiJavaFileStub calcStub() {
        final PsiJavaFileStubImpl answer = new PsiJavaFileStubImpl(CodegenUtil.getFQName(file.getRootNamespace()), true);
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
            protected void generateNamespace(JetNamespace namespace) {
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
                        LOG.error("Unbalanced stack operations");
                    }
                }
            }
        };


        List<PsiFile> files = Collections.<PsiFile>singletonList(file);
        final BindingContext context = AnalyzerFacade.shallowAnalyzeFiles(files);
        state.compileCorrectNamespaces(context, AnalyzerFacade.collectRootNamespaces(files));
        state.getFactory().files();

        return answer;
    }
    
}
