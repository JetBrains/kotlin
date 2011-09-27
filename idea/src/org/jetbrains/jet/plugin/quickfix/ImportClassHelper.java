package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetPluginUtil;

import java.util.List;

/**
 * @author svtk
 */
public class ImportClassHelper {
    public static void perform(@NotNull JetType type, @NotNull PsiElement element, @NotNull PsiElement newElement) {
        PsiElement parent = element;
        while (!(parent instanceof JetNamespace)) {
            parent = parent.getParent();
            assert parent != null;
        }
        JetNamespace namespace = (JetNamespace) parent;
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();

        if (JetPluginUtil.checkTypeIsStandard(type, element.getProject())) {
            element.replace(newElement);
            return;
        }

        String typeFullName = JetPluginUtil.computeTypeFullName(type);

        JetImportDirective newDirective = JetPsiFactory.createImportDirective(element.getProject(), typeFullName);

        if (!importDirectives.isEmpty()) {
            boolean isPresent = false;
            for (JetImportDirective directive : importDirectives) {
                if (directive.getText().endsWith(typeFullName) ||
                    directive.getText().endsWith(typeFullName + ";")) {
                    isPresent = true;
                }
            }
            if (!isPresent) {
                JetImportDirective lastDirective = importDirectives.get(importDirectives.size() - 1);
                lastDirective.getParent().addAfter(newDirective, lastDirective);
                lastDirective.getParent().addAfter(JetPsiFactory.createWhiteSpace(element.getProject(), "\n"), lastDirective);
            }
        }
        else {
            List<JetDeclaration> declarations = namespace.getDeclarations();
            assert !declarations.isEmpty();
            JetDeclaration firstDeclaration = declarations.iterator().next();
            firstDeclaration.getParent().addBefore(newDirective, firstDeclaration);
            firstDeclaration.getParent().addBefore(JetPsiFactory.createWhiteSpace(element.getProject(), "\n\n"), firstDeclaration);
        }
        element.replace(newElement);
        parent.replace(namespace);
    }
}
