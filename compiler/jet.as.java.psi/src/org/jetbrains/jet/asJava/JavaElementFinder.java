/*
 * @author max
 */
package org.jetbrains.jet.asJava;

import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.*;

public class JavaElementFinder extends PsiElementFinder {
    private final Project project;
    private final PsiManager psiManager;

    private WeakHashMap<GlobalSearchScope, List<JetFile>> jetFiles = new WeakHashMap<GlobalSearchScope, List<JetFile>>();

    public JavaElementFinder(Project project) {
        this.project = project;
        psiManager = PsiManager.getInstance(project);

        // Monitoring for files instead of collecting them each time
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void fileCreated(VirtualFileEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void fileDeleted(VirtualFileEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void fileMoved(VirtualFileMoveEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void fileCopied(VirtualFileCopyEvent event) {
                invalidateJetFilesCache();
            }
        });
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
        final List<JetFile> filesInScope = collectProjectJetFiles(project, scope);
        for (JetFile file : filesInScope) {
            final String packageName = JetPsiUtil.getFQName(file);
            if (packageName != null && qualifiedName.startsWith(packageName)) {
                if (qualifiedName.equals(fqn(packageName, JvmAbi.PACKAGE_CLASS))) {
                    answer.add(new JetLightClass(psiManager, file, qualifiedName));
                }
                else {
                    for (JetDeclaration declaration : file.getDeclarations()) {
                        scanClasses(answer, declaration, qualifiedName, packageName, file);
                    }
                }
            }
        }
        return answer.toArray(new PsiClass[answer.size()]);
    }

    private void scanClasses(List<PsiClass> answer, JetDeclaration declaration, String qualifiedName, String containerFqn, JetFile file) {
        if (declaration instanceof JetClassOrObject) {
            String localName = getLocalName(declaration);
            if (localName != null) {
                String fqn = fqn(containerFqn, localName);
                if (qualifiedName.equals(fqn)) {
                    answer.add(new JetLightClass(psiManager, file, qualifiedName));
                }
                else {
                    for (JetDeclaration child : ((JetClassOrObject) declaration).getDeclarations()) {
                        scanClasses(answer, child, qualifiedName, fqn, file);
                    }
                }
            }
        }
        else if (declaration instanceof JetClassObject) {
            scanClasses(answer, ((JetClassObject) declaration).getObjectDeclaration(), qualifiedName, containerFqn, file);
        }
    }

    private static String getLocalName(JetDeclaration declaration) {
        String given = declaration.getName();
        if (given != null) return given;

        if (declaration instanceof JetObjectDeclaration) {
            return JetTypeMapper.getLocalNameForObject((JetObjectDeclaration) declaration);
        }
        return null;
    }

    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        Set<String> answer = new HashSet<String>();

        String packageFQN = psiPackage.getQualifiedName();
        for (JetFile psiFile : collectProjectJetFiles(project, GlobalSearchScope.allScope(project))) {
            if (packageFQN.equals(JetPsiUtil.getFQName(psiFile))) {
                answer.add(JvmAbi.PACKAGE_CLASS);
                for (JetDeclaration declaration : psiFile.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        answer.add(getLocalName(declaration));
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
        final List<JetFile> psiFiles = collectProjectJetFiles(project, GlobalSearchScope.allScope(project));

        for (JetFile psiFile : psiFiles) {
            if (qualifiedName.equals(JetPsiUtil.getFQName(psiFile))) {
                return new PsiPackageImpl(psiFile.getManager(), qualifiedName);
            }
        }

        return null;
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<PsiClass>();
        final List<JetFile> filesInScope = collectProjectJetFiles(project, scope);
        String packageFQN = psiPackage.getQualifiedName();
        for (JetFile file : filesInScope) {
            if (packageFQN.equals(JetPsiUtil.getFQName(file))) {
                answer.add(new JetLightClass(psiManager, file, fqn(packageFQN, JvmAbi.PACKAGE_CLASS)));
                for (JetDeclaration declaration : file.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        answer.add(new JetLightClass(psiManager, file, fqn(packageFQN, getLocalName(declaration))));
                    }
                }
            }
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }

    private synchronized void invalidateJetFilesCache() {
        jetFiles.clear();
    }

    private synchronized List<JetFile> collectProjectJetFiles(final Project project, @NotNull final GlobalSearchScope scope) {
        List<JetFile> cachedFiles = jetFiles.get(scope);
        
        if (cachedFiles == null) {
            final List<JetFile> answer = new ArrayList<JetFile>();

            final FileTypeManager fileTypeManager = FileTypeManager.getInstance();

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

            cachedFiles = answer;
             jetFiles.put(scope, answer);
        }

        return cachedFiles;
    }
}

