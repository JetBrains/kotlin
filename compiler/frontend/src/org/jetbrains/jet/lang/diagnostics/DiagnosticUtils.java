package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public class DiagnosticUtils {
    private DiagnosticUtils() {
    }

    public static String atLocation(@NotNull PsiElement element) {
        return atLocation(element.getNode());
    }

    public static String atLocation(@NotNull ASTNode node) {
        int startOffset = node.getStartOffset();
        PsiElement element = getClosestPsiElement(node);
        if (element != null) {
            return atLocation(element.getContainingFile(), element.getTextRange());
        }
        return "' at offset " + startOffset + " (line and file unknown)";
    }

    @Nullable
    public static PsiElement getClosestPsiElement(@NotNull ASTNode node) {
        while (node.getPsi() == null) {
            node = node.getTreeParent();
        }
        return node.getPsi();
    }
    
    @NotNull
    public static PsiFile getContainingFile(@NotNull ASTNode node) {
        PsiElement closestPsiElement = getClosestPsiElement(node);
        assert closestPsiElement != null : "This node is not contained by a file";
        return closestPsiElement.getContainingFile();
    }

    @NotNull
    public static String atLocation(@NotNull PsiFile file, @NotNull TextRange textRange) {
        Document document = file.getViewProvider().getDocument();//PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        return atLocation(file, textRange, document);
    }

    @NotNull
    public static String atLocation(PsiFile file, TextRange textRange, Document document) {
        int offset = textRange.getStartOffset();
        VirtualFile virtualFile = file.getVirtualFile();
        String pathSuffix = virtualFile == null ? "" : " in " + virtualFile.getPath();
        if (document != null) {
            int lineNumber = document.getLineNumber(offset);
            int lineStartOffset = document.getLineStartOffset(lineNumber);
            int column = offset - lineStartOffset;

            return "' at line " + (lineNumber + 1) + ":" + (column + 1) + pathSuffix;
        }
        else {
            return "' at offset " + offset + " (line unknown)" + pathSuffix;
        }
    }

    public static String formatPosition(DiagnosticWithTextRange diagnosticWithTextRange) {
        PsiFile file = diagnosticWithTextRange.getPsiFile();
        Document document = file.getViewProvider().getDocument();
        String position;
        int offset = diagnosticWithTextRange.getTextRange().getStartOffset();
        if (document != null) {
            int lineNumber = document.getLineNumber(offset);
            int lineStartOffset = document.getLineStartOffset(lineNumber);
            int column = offset - lineStartOffset;

            position = "(" + (lineNumber + 1) + "," + (column + 1) + ")";
        }
        else {
            position = "(offset: " + offset + " line unknown)";
        }
        return position;
    }

    public static void throwIfRunningOnServer(Throwable e) {
        // This is needed for the Web Demo server to log the exceptions coming from the analyzer instead of showing them in the editor.
        if (System.getProperty("kotlin.running.in.server.mode", "false").equals("true") || ApplicationManager.getApplication().isUnitTestMode()) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new RuntimeException(e);
        }
    }
}
