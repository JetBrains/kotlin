/*
 * @author max
 */
package org.jetbrains.jet.plugin.java;

import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaElementFinder extends PsiElementFinder {
    private final Project project;
    private final PsiManager psiManager;

    public JavaElementFinder(Project project) {
        this.project = project;
        psiManager = PsiManager.getInstance(project);
    }

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
        final List<JetFile> filesInScope = collectProjectFiles(project, scope);
        for (JetFile file : filesInScope) {
            JetNamespace rootNamespace = file.getRootNamespace();
            final String packageName = JetPsiUtil.getFQName(rootNamespace);
            if (packageName != null && qualifiedName.startsWith(packageName)) {
                if (qualifiedName.equals(fqn(packageName, "namespace"))) {
                    answer.add(new JetLightClass(psiManager, file, "namespace"));
                }
                
                for (JetDeclaration declaration : rootNamespace.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        if (qualifiedName.equals(fqn(packageName, declaration.getName()))) {
                            answer.add(new JetLightClass(psiManager, file, declaration.getName()));
                        }
                    }
                }
            }
        }
        return answer.toArray(new PsiClass[answer.size()]);
    }

    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        Set<String> answer = new HashSet<String>();

        String packageFQN = psiPackage.getQualifiedName();
        for (JetFile psiFile : collectProjectFiles(project, GlobalSearchScope.allScope(project))) {
            final JetNamespace rootNamespace = psiFile.getRootNamespace();
            if (packageFQN.equals(JetPsiUtil.getFQName(rootNamespace))) {
                answer.add("namespace");
                for (JetDeclaration declaration : rootNamespace.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        answer.add(declaration.getName());
                    }
                }
            }
        }

        return answer;
    }

    private static String fqn(String packageName, String className) {
        if (StringUtil.isEmpty(packageName)) return className;
        return packageName + "." + className;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedName) {
        final List<JetFile> psiFiles = collectProjectFiles(project, GlobalSearchScope.allScope(project));

        for (JetFile psiFile : psiFiles) {
            if (qualifiedName.equals(JetPsiUtil.getFQName(psiFile.getRootNamespace()))) {
                return new PsiPackageImpl(psiFile.getManager(), qualifiedName);
            }
        }

        return null;
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<PsiClass>();
        final List<JetFile> filesInScope = collectProjectFiles(project, scope);
        String packageFQN = psiPackage.getQualifiedName();
        for (JetFile file : filesInScope) {
            final JetNamespace rootNamespace = file.getRootNamespace();
            if (packageFQN.equals(JetPsiUtil.getFQName(rootNamespace))) {
                answer.add(new JetLightClass(psiManager, file, "namespace"));
                for (JetDeclaration declaration : rootNamespace.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        answer.add(new JetLightClass(psiManager, file, declaration.getName()));
                    }
                }
            }
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }


    private static List<JetFile> collectProjectFiles(final Project project, @NotNull final GlobalSearchScope scope) {
        final List<JetFile> answer = new ArrayList<JetFile>();


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
                        answer.add((JetFile) psiFile);
                    }
                }
            }
        });

        return answer;
    }
}

