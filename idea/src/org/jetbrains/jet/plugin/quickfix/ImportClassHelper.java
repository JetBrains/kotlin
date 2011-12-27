package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetPluginUtil;

import java.util.List;

/**
 * @author svtk
 */
public class ImportClassHelper {
    public static void perform(@NotNull JetType type, @NotNull PsiElement elementToReplace, @NotNull PsiElement newElement) {
        if (JetPluginUtil.checkTypeIsStandard(type, elementToReplace.getProject()) || ErrorUtils.isError(type.getMemberScope().getContainingDeclaration())) {
            elementToReplace.replace(newElement);
            return;
        }
        perform(JetPluginUtil.computeTypeFullName(type), elementToReplace, newElement);
    }
    
    public static void perform(@NotNull String typeFullName, @NotNull JetFile namespace) {
        perform(typeFullName, namespace, null, null);
    }

    /**
     * Get the outer namespace PSI element for given element in the tree.
     *
     * @param element Some element in the tree.
     * @return A namespace element in the tree.
     */
    public static JetFile findOuterNamespace(@NotNull PsiElement element) {
        PsiElement parent = element;
        while (!(parent instanceof JetFile)) {
            parent = parent.getParent();
            assert parent != null;
        }

        return (JetFile) parent;
    }

    public static void perform(@NotNull String typeFullName, @NotNull PsiElement elementToReplace, @NotNull PsiElement newElement) {
        perform(typeFullName, findOuterNamespace(elementToReplace), elementToReplace, newElement);
    }

    /**
     * Add import directive into the PSI tree for the given namespace.
     *
     * @param importString full name of the import. Can contain .* if necessary.
     * @param namespace Namespace where directive should be added.
     */
    public static void addImportDirective(@NotNull String importString, @NotNull JetFile namespace) {
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();

        JetImportDirective newDirective = JetPsiFactory.createImportDirective(namespace.getProject(), importString);

        String lineSeparator = System.getProperty("line.separator");
        if (!importDirectives.isEmpty()) {

            // Check if import is already present
            for (JetImportDirective directive : importDirectives) {
                if (directive.getText().endsWith(importString) || directive.getText().endsWith(importString + ";")) {
                    return;
                }
            }

            JetImportDirective lastDirective = importDirectives.get(importDirectives.size() - 1);
            lastDirective.getParent().addAfter(newDirective, lastDirective);
            lastDirective.getParent().addAfter(JetPsiFactory.createWhiteSpace(namespace.getProject(), lineSeparator), lastDirective);
        }
        else {
            List<JetDeclaration> declarations = namespace.getDeclarations();
            assert !declarations.isEmpty();
            JetDeclaration firstDeclaration = declarations.iterator().next();
            firstDeclaration.getParent().addBefore(newDirective, firstDeclaration);
            firstDeclaration.getParent().addBefore(JetPsiFactory.createWhiteSpace(namespace.getProject(), lineSeparator + lineSeparator), firstDeclaration);
        }
    }

    public static void perform(@NotNull String typeFullName, @NotNull JetFile namespace, @Nullable PsiElement elementToReplace, @Nullable PsiElement newElement) {
        addImportDirective(typeFullName, namespace);

        if (elementToReplace != null && newElement != null && elementToReplace != newElement) {
            elementToReplace.replace(newElement);
        }
    }
}
