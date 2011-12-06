/*
 * @author max
 */
package org.jetbrains.jet.plugin.java;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.SmartList;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.CodegenUtil;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.*;

public class JavaElementFinder extends PsiElementFinder {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.java.JavaElementFinder");
    
    private final Project project;

    public JavaElementFinder(Project project) {
        this.project = project;
    }

    private final static Key<PsiJavaFileStub> JAVA_API_STUB = Key.create("JAVA_API_STUB");
    
    @Override
    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        final PsiClass[] allClasses = findClasses(qualifiedName, scope);
        return allClasses.length > 0 ? allClasses[0] : null;
    }

    @NotNull
    @Override
    public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        // Backend searches for java.lang.String. Will fail with SOE if continue
        if (qualifiedName.startsWith("java.")) return PsiClass.EMPTY_ARRAY;

        List<PsiClass> answer = new SmartList<PsiClass>();
        final List<PsiFile> filesInScope = ensureUpToDate(scope);
        for (PsiFile file : filesInScope) {
            final PsiJavaFileStub stub = file.getUserData(JAVA_API_STUB);
            if (stub == null) continue;

            final String packageName = stub.getPackageName();
            if (packageName != null && qualifiedName.startsWith(packageName)) {
                collectClasses(answer, qualifiedName, stub);
            }
        }
        return answer.toArray(new PsiClass[answer.size()]);
    }

    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        Set<String> answer = new HashSet<String>();

        String packageFQN = psiPackage.getQualifiedName();
        for (PsiFile psiFile : collectProjectFiles(project, GlobalSearchScope.allScope(project))) {
            if (psiFile instanceof JetFile) {
                final JetNamespace rootNamespace = ((JetFile) psiFile).getRootNamespace();
                if (packageFQN.equals(CodegenUtil.getFQName(rootNamespace))) {
                    answer.add("namespace");
                    for (JetDeclaration declaration : rootNamespace.getDeclarations()) {
                        if (declaration instanceof JetClassOrObject) {
                            answer.add(declaration.getName());
                        }
                    }
                }
            }
        }

        return answer;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedName) {
        final List<PsiFile> psiFiles = collectProjectFiles(project, GlobalSearchScope.allScope(project));

        for (PsiFile psiFile : psiFiles) {
            if (psiFile instanceof JetFile) {
                if (qualifiedName.equals(CodegenUtil.getFQName(((JetFile) psiFile).getRootNamespace()))) {
                    return new PsiPackageImpl(psiFile.getManager(), qualifiedName);
                }
            }
        }

        return null;
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<PsiClass>();
        final List<PsiFile> filesInScope = ensureUpToDate(scope);
        final String qualifiedName = psiPackage.getQualifiedName();
        for (PsiFile file : filesInScope) {
            final PsiJavaFileStub stub = file.getUserData(JAVA_API_STUB);
            if (stub == null) continue;

            if (Comparing.equal(qualifiedName, stub.getPackageName())) {
                for (StubElement child : stub.getChildrenStubs()) {
                    if (child instanceof PsiClassStub) {
                        answer.add((PsiClass) child.getPsi());
                    }
                }
            }
        }
        return answer.toArray(new PsiClass[answer.size()]);
    }

    private static void collectClasses(List<PsiClass> answer, String qualifiedName, StubElement<?> stub) {
        if (stub instanceof PsiClassStub && qualifiedName.equals(((PsiClassStub) stub).getQualifiedName())) {
            answer.add((PsiClass) stub.getPsi());
        }

        for (StubElement child : stub.getChildrenStubs()) {
            collectClasses(answer, qualifiedName, child);
        }
    }

    private List<PsiFile> ensureUpToDate(GlobalSearchScope scope) {
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
                final PsiJavaFileStubImpl fileStub = new PsiJavaFileStubImpl(CodegenUtil.getFQName(namespace), true);
                PsiManager manager = PsiManager.getInstance(project);
                stubStack.push(fileStub);

                final PsiFile file = namespace.getContainingFile();
                file.putUserData(JAVA_API_STUB, fileStub);
                fileStub.setPsiFactory(new ClsWrapperStubPsiFactory());
                final ClsFileImpl fakeFile = new ClsFileImpl((PsiManagerImpl) manager, new ClassFileViewProvider(manager, file.getVirtualFile())) {
                    @NotNull
                    @Override
                    public PsiClassHolderFileStub getStub() {
                        return fileStub;
                    }
                };

                fakeFile.setPhysical(false);
                fileStub.setPsi(fakeFile);

                try {
                    super.generateNamespace(namespace);
                }
                finally {
                    final StubElement pop = stubStack.pop();
                    if (pop != fileStub) {
                        LOG.error("Unbalanced stack operations");
                    }
                }
            }
        };

        final List<PsiFile> psiFiles = collectProjectFiles(project, scope);
        
        Collection<PsiFile> dirty = Collections2.filter(psiFiles, new Predicate<PsiFile>() {
            @Override
            public boolean apply(PsiFile psiFile) {
                return psiFile.getUserData(JAVA_API_STUB) == null;
            }
        });
                

        if (dirty.size() > 0) {
            final BindingContext context = AnalyzerFacade.shallowAnalyzeFiles(dirty);
            state.compileCorrectNamespaces(context, AnalyzerFacade.collectRootNamespaces(dirty));
            state.getFactory().files();
        }
        
        return psiFiles;
    }

    private static List<PsiFile> collectProjectFiles(final Project project, @NotNull final GlobalSearchScope scope) {
        final List<PsiFile> answer = new ArrayList<PsiFile>();


        final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        final PsiManager psiManager = PsiManager.getInstance(project);

        VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
        CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor() {
            @Override
            protected void acceptFile(VirtualFile file, String fileRoot, String filePath) {
                final FileType fileType = fileTypeManager.getFileTypeByFile(file);
                if (fileType != JetFileType.INSTANCE) return;

                if (scope.accept(file)) {
                    final PsiFile psiFile = psiManager.findFile(file);
                    if (psiFile instanceof JetFile) {
                        answer.add(psiFile);
                    }
                }
            }
        });

        return answer;
    }
}

