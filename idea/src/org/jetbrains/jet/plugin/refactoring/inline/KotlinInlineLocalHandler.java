package org.jetbrains.jet.plugin.refactoring.inline;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.lang.Language;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringMessageDialog;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.Collection;
import java.util.Set;

public class KotlinInlineLocalHandler extends InlineActionHandler {
    @Override
    public boolean isEnabledForLanguage(Language l) {
        return l.equals(JetLanguage.INSTANCE);
    }

    @Override
    public boolean canInlineElement(PsiElement element) {
        if (!(element instanceof JetProperty)) {
            return false;
        }
        JetProperty property = (JetProperty) element;
        return !property.isVar();
    }

    @Override
    public void inlineElement(final Project project, Editor editor, PsiElement element) {
        final JetProperty val = (JetProperty) element;
        String name = val.getName();

        JetExpression initializerInDeclaration = val.getInitializer();

        final Collection<PsiReference> references = ReferencesSearch.search(val, GlobalSearchScope.allScope(project), false).findAll();

        final Set<PsiElement> assignments = Sets.newHashSet();
        for (PsiReference ref : references) {
            PsiElement refElement = ref.getElement();
            PsiElement parent = refElement.getParent();
            if (parent instanceof JetBinaryExpression &&
                ((JetBinaryExpression) parent).getOperationToken() == JetTokens.EQ &&
                ((JetBinaryExpression) parent).getLeft() == refElement) {
                assignments.add(parent);
            }
        }

        final JetExpression initializer;
        if (initializerInDeclaration != null) {
            initializer = initializerInDeclaration;
        }
        else {
            if (assignments.size() == 1) {
                initializer = ((JetBinaryExpression) assignments.iterator().next()).getRight();
            }
            else {
                initializer = null;
            }
            if (initializer == null) {
                String key = assignments.isEmpty() ? "variable.has.no.initializer" : "variable.has.no.dominating.definition";
                String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name));
                CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("inline.variable.title"), HelpID.INLINE_VARIABLE);
                return;
            }
        }

        PsiReference[] referencesArray = references.toArray(references.toArray(new PsiReference[references.size()]));

        if (editor != null && !ApplicationManager.getApplication().isUnitTestMode()) {
            EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
            TextAttributes attributes = editorColorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
            HighlightManager.getInstance(project).addOccurrenceHighlights(editor, referencesArray, attributes, true, null);
            RefactoringMessageDialog dialog = new RefactoringMessageDialog(
                    RefactoringBundle.message("inline.variable.title"),
                    RefactoringBundle.message("inline.local.variable.prompt", name) + " " +
                                      RefactoringBundle.message("occurences.string", references.size()),
                    HelpID.INLINE_VARIABLE,
                    "OptionPane.questionIcon",
                    true,
                    project);

            dialog.show();
            if (!dialog.isOK()){
                StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                if (statusBar != null) {
                    statusBar.setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
                }
                return;
            }
        }

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        for (PsiReference reference : references) {
                            PsiElement referenceElement = reference.getElement();
                            if (assignments.contains(referenceElement.getParent())) {
                                continue;
                            }

                            if (referenceElement.getParent() instanceof JetSimpleNameStringTemplateEntry &&
                                !(initializer instanceof JetSimpleNameExpression)) {
                                referenceElement.getParent().replace(JetPsiFactory.createBlockStringTemplateEntry(project, initializer));
                            }
                            else {
                                referenceElement.replace(initializer.copy());
                            }
                        }

                        for (PsiElement assignment : assignments) {
                            assignment.delete();
                        }

                        val.delete();
                    }
                });
            }
        }, RefactoringBundle.message("inline.command", name), null);
    }
}
