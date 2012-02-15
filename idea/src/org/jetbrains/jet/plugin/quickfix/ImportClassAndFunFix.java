/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
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
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.actions.JetAddImportAction;
import org.jetbrains.jet.plugin.caches.JetCacheManager;

import java.util.*;

/**
 * Check possibility and perform fix for unresolved references.
 *
 * @author Nikolay Krasko
 */
public class ImportClassAndFunFix extends JetHintAction<JetSimpleNameExpression> implements HighPriorityAction {

    @NotNull
    private final List<String> suggestions;

    public ImportClassAndFunFix(@NotNull JetSimpleNameExpression element) {
        super(element);
        suggestions = computeSuggestions(element);
    }

    private static List<String> computeSuggestions(@NotNull JetSimpleNameExpression element) {
        final PsiFile file = element.getContainingFile();
        if (!(file instanceof JetFile)) {
            return Collections.emptyList();
        }

        final ArrayList<String> result = new ArrayList<String>();
        result.addAll(getClassNames(element, file.getProject()));
        result.addAll(getJetTopLevelFunctions(element, file.getProject()));

        return result;
    }
    
    private static Collection<String> getJetTopLevelFunctions(@NotNull JetSimpleNameExpression expression, @NotNull Project project) {
        final String referenceName = expression.getReferencedName();

        if (referenceName == null) {
            return Collections.emptyList();
        }

        final Collection<JetNamedFunction> namedFunctions =
                JetCacheManager.getInstance(project).getNamesCache().getTopLevelFunctionsByName(
                        referenceName, GlobalSearchScope.allScope(project));

        final Collection<String> nullableNames =
                Collections2.transform(Lists.newArrayList(namedFunctions), new Function<JetNamedFunction, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable JetNamedFunction jetFunction) {
                        return jetFunction != null ? jetFunction.getQualifiedName() : null;
                    }
                });

        return Collections2.filter(nullableNames, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String fqn) {
                return fqn != null && !fqn.isEmpty();
            }
        });
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

        PsiClass[] classes = cache.getClassesByName(typeName, new DelegatingGlobalSearchScope(scope) {
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
                    return new ImportClassAndFunFix(psiElement);
                }

                return null;
            }
        };
    }}
