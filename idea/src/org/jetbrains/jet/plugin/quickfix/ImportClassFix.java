package org.jetbrains.jet.plugin.quickfix;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.actions.JetAddImportAction;
import org.jetbrains.jet.plugin.caches.JetCacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Check possibility and perform fix for unresolved references.
 *
 * @author Nikolay Krasko
 */
public class ImportClassFix extends JetHintAction<JetSimpleNameExpression> implements HighPriorityAction {

    @NotNull
    private final List<String> suggestions;

    public ImportClassFix(@NotNull JetSimpleNameExpression element) {
        super(element);
        suggestions = computeSuggestions(element);
    }

    private static List<String> computeSuggestions(@NotNull JetSimpleNameExpression element) {
        final PsiFile file = element.getContainingFile();
        if (!(file instanceof JetFile)) {
            return Collections.emptyList();
        }

        return getClassNames(element, file.getProject());
    }

    /*
     * Searches for possible class names in kotlin context and java facade.
     */
    public static List<String> getClassNames(@NotNull JetSimpleNameExpression expression, @NotNull Project project) {
        final String referenceName = expression.getReferencedName();

        if (referenceName == null) {
            return Collections.emptyList();
        }

        final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Set<String> possibleResolveNames = Sets.newHashSet();
        possibleResolveNames.addAll(JetCacheManager.getInstance(project).getNamesCache().getFQNamesByName(referenceName, scope));
        possibleResolveNames.addAll(getJavaClasses(referenceName, project, scope));

        // TODO: Do appropriate sorting
        return Lists.newArrayList(possibleResolveNames);
    }

    private static Collection<String> getJavaClasses(@NotNull final String typeName, @NotNull Project project, final GlobalSearchScope scope) {
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);

        PsiClass[] classes = cache.getClassesByName(typeName, new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            @Override
            public boolean contains(@NotNull VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }
        });

        return Collections2.transform(Lists.newArrayList(classes), new Function<PsiClass, String>() {
            @Nullable
            @Override
            public String apply(@Nullable PsiClass javaClass) {
                assert javaClass != null;
                return javaClass.getQualifiedName();
            }
        });
    }

    @Override
    public boolean showHint(@NotNull Editor editor) {
        if (suggestions.isEmpty()) {
            return false;
        }

        final Project project = editor.getProject();
        if (project == null) {
            return false;
        }
        
        String hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, suggestions.get(0));
        if (!ApplicationManager.getApplication().isUnitTestMode() &&
                !HintManager.getInstance().hasShownHintsThatWillHideByOtherHint()) {

            HintManager.getInstance().showQuestionHint(
                    editor, hintText,
                    element.getTextOffset(), element.getTextRange().getEndOffset(),
                    createAction(project, editor));
        }

        return true;
    }

    @Override
    @NotNull
    public String getText() {
        return QuickFixBundle.message("import.class.fix");
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return QuickFixBundle.message("import.class.fix");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !suggestions.isEmpty();
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, final PsiFile file) throws IncorrectOperationException {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                createAction(project, editor).execute();
            }
        });
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @NotNull
    private JetAddImportAction createAction(@NotNull Project project, @NotNull Editor editor) {
        return new JetAddImportAction(project, editor, element, suggestions);
    }

    @Nullable
    public static JetIntentionActionFactory<JetSimpleNameExpression> createFactory() {
        return new JetIntentionActionFactory<JetSimpleNameExpression>() {
            @Nullable
            @Override
            public JetIntentionAction<JetSimpleNameExpression> createAction(@NotNull DiagnosticWithPsiElement diagnostic) {
                // There could be different psi elements (i.e. JetArrayAccessExpression), but we can fix only JetSimpleNameExpression case
                if (diagnostic.getPsiElement() instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression psiElement = (JetSimpleNameExpression) diagnostic.getPsiElement();
                    return new ImportClassFix(psiElement);
                }

                return null;
            }
        };
    }}
