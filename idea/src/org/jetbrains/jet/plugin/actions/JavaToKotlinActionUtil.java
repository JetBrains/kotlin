package org.jetbrains.jet.plugin.actions;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ex.MessagesEx;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.visitors.ClassVisitor;

import java.io.IOException;
import java.util.*;

/**
 * @author ignatov
 */
public class JavaToKotlinActionUtil {
    private JavaToKotlinActionUtil() {
    }

    static void setClassIdentifiers(@NotNull PsiFile psiFile) {
        ClassVisitor c = new ClassVisitor();
        psiFile.accept(c);
        Converter.setClassIdentifiers(c.getClassIdentifiers());
    }

    @NotNull
    private static List<VirtualFile> getChildrenRecursive(@Nullable VirtualFile baseDir) {
        List<VirtualFile> result = new LinkedList<VirtualFile>();
        VirtualFile[] children = baseDir != null ? baseDir.getChildren() : VirtualFile.EMPTY_ARRAY;
        result.addAll(Arrays.asList(children));
        for (VirtualFile f : children)
            result.addAll(getChildrenRecursive(f));
        return result;
    }

    @NotNull
    static List<PsiFile> getAllJavaFiles(@NotNull VirtualFile[] vFiles, Project project) {
        final PsiManager manager = PsiManager.getInstance(project);
        Set<VirtualFile> filesSet = new HashSet<VirtualFile>();
        for (VirtualFile f : vFiles) {
            filesSet.add(f);
            filesSet.addAll(getChildrenRecursive(f));
        }
        final List<PsiFile> res = new ArrayList<PsiFile>();
        for (final VirtualFile file : filesSet) {
            final PsiFile psiFile = manager.findFile(file);
            if (psiFile != null && psiFile.getFileType() instanceof JavaFileType) {
                res.add(psiFile);
            }
        }
        return res;
    }

    static void reformatFiles(List<VirtualFile> allJetFiles, final Project project) {
        for (final VirtualFile vf : allJetFiles)
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    if (vf != null) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                        if (psiFile != null) {
                            CodeStyleManager.getInstance(project).reformat(psiFile);
                        }
                    }
                }
            });
    }

    @NotNull
    static List<VirtualFile> performFiles(List<PsiFile> allJavaFilesNear) {
        final List<VirtualFile> result = new LinkedList<VirtualFile>();
        for (final PsiFile f : allJavaFilesNear) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    VirtualFile vf = performOneFile(f);
                    if (vf != null) {
                        result.add(vf);
                    }
                }
            });
        }
        return result;
    }

    static void deleteFiles(List<PsiFile> allJavaFilesNear) {
        for (final PsiFile f : allJavaFilesNear) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        PsiManager manager = f.getManager();
                        VirtualFile vFile = f.getVirtualFile();
                        if (vFile != null) {
                            vFile.delete(manager);
                        }
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }

    @Nullable
    private static VirtualFile performOneFile(PsiFile psiFile) {
        try {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (psiFile instanceof PsiJavaFile && virtualFile != null) {
                String result = "";
                try {
                    result = Converter.fileToFile((PsiJavaFile) psiFile).toKotlin();
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                final PsiManager manager = psiFile.getManager();
                assert manager != null;
                VirtualFile copy = virtualFile.copy(manager, virtualFile.getParent(), virtualFile.getNameWithoutExtension() + ".kt");
                copy.setBinaryContent(CharsetToolkit.getUtf8Bytes(result));
                return copy;
            }
        } catch (Exception ex) {
            MessagesEx.error(psiFile.getProject(), ex.getMessage()).showLater();
        }
        return null;
    }

    static void renameFiles(@NotNull List<PsiFile> psiFiles) {
        for (final PsiFile f : psiFiles) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        PsiManager manager = f.getManager();
                        VirtualFile vFile = f.getVirtualFile();
                        if (vFile != null) {
                            vFile.copy(manager, vFile.getParent(), vFile.getNameWithoutExtension() + ".java.old");
                            vFile.delete(manager);
                        }
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }
}
