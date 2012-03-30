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
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.actions.JetAddImportAction;
import org.jetbrains.jet.plugin.caches.JetCacheManager;
import org.jetbrains.jet.plugin.caches.JetShortNamesCache;

import java.util.*;

/**
 * Check possibility and perform fix for unresolved references.
 *
 * @author Nikolay Krasko
 */
public class ImportClassAndFunFix extends JetHintAction<JetSimpleNameExpression> implements HighPriorityAction {

    @NotNull
    private final Collection<FqName> suggestions;

    public ImportClassAndFunFix(@NotNull JetSimpleNameExpression element) {
        super(element);
        suggestions = computeSuggestions(element);
    }

    private static Collection<FqName> computeSuggestions(@NotNull JetSimpleNameExpression element) {
        final PsiFile file = element.getContainingFile();
        if (!(file instanceof JetFile)) {
            return Collections.emptyList();
        }

        final String referenceName = element.getReferencedName();

        if (!StringUtil.isNotEmpty(referenceName)) {
            return Collections.emptyList();
        }

        assert referenceName != null;

        List<FqName> result = Lists.newArrayList();
        result.addAll(getClassNames(referenceName, file.getProject()));
        result.addAll(getJetTopLevelFunctions(referenceName, element, file.getProject()));
        result.addAll(getJetExtensionFunctions(referenceName, element, file.getProject()));

        return Collections2.filter(result, new Predicate<FqName>() {
            @Override
            public boolean apply(@Nullable FqName fqName) {
                assert fqName != null;
                return ImportInsertHelper.doNeedImport(new ImportPath(fqName, false), null, (JetFile) file);
            }
        });
    }
    
    private static Collection<FqName> getJetTopLevelFunctions(@NotNull String referenceName, JetSimpleNameExpression expression, @NotNull Project project) {
        JetShortNamesCache namesCache = JetCacheManager.getInstance(project).getNamesCache();
        Collection<FunctionDescriptor> topLevelFunctions = namesCache.getTopLevelFunctionDescriptorsByName(
                referenceName,
                expression,
                GlobalSearchScope.allScope(project));

        return Sets.newHashSet(Collections2.transform(topLevelFunctions, new Function<DeclarationDescriptor, FqName>() {
            @Override
            public FqName apply(@Nullable DeclarationDescriptor declarationDescriptor) {
                assert declarationDescriptor != null;
                return DescriptorUtils.getFQName(declarationDescriptor).toSafe();
            }
        }));
    }

    private static Collection<FqName> getJetExtensionFunctions(
            @NotNull final String referenceName,
            @NotNull JetSimpleNameExpression expression,
            @NotNull Project project
    ) {
        JetShortNamesCache namesCache = JetCacheManager.getInstance(project).getNamesCache();
        Collection<DeclarationDescriptor> jetCallableExtensions = namesCache.getJetCallableExtensions(
                new Condition<String>() {
                    @Override
                    public boolean value(String callableExtensionName) {
                        return callableExtensionName.equals(referenceName);
                    }
                },
                expression,
                GlobalSearchScope.allScope(project));

        return Sets.newHashSet(Collections2.transform(jetCallableExtensions, new Function<DeclarationDescriptor, FqName>() {
            @Override
            public FqName apply(@Nullable DeclarationDescriptor declarationDescriptor) {
                assert declarationDescriptor != null;
                return DescriptorUtils.getFQName(declarationDescriptor).toSafe();
            }
        }));
    }

    /*
     * Searches for possible class names in kotlin context and java facade.
     */
    public static Collection<FqName> getClassNames(@NotNull String referenceName, @NotNull Project project) {
        final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Set<FqName> possibleResolveNames = Sets.newHashSet();
        possibleResolveNames.addAll(JetCacheManager.getInstance(project).getNamesCache().getFQNamesByName(referenceName, scope));
        possibleResolveNames.addAll(getJavaClasses(referenceName, project, scope));

        // TODO: Do appropriate sorting
        return Lists.newArrayList(possibleResolveNames);
    }

    private static Collection<FqName> getJavaClasses(@NotNull final String typeName, @NotNull Project project, final GlobalSearchScope scope) {
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);

        PsiClass[] classes = cache.getClassesByName(typeName, new DelegatingGlobalSearchScope(scope) {
            @Override
            public boolean contains(@NotNull VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }
        });

        return Collections2.transform(Lists.newArrayList(classes), new Function<PsiClass, FqName>() {
            @Nullable
            @Override
            public FqName apply(@Nullable PsiClass javaClass) {
                assert javaClass != null;
                return new FqName(javaClass.getQualifiedName());
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

        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) {
            return false;
        }

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            String hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, suggestions.iterator().next().getFqName());

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
        return JetBundle.message("import.fix");
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return JetBundle.message("import.fix");
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
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction<JetSimpleNameExpression> createAction(@NotNull Diagnostic diagnostic) {
                // There could be different psi elements (i.e. JetArrayAccessExpression), but we can fix only JetSimpleNameExpression case
                if (diagnostic.getPsiElement() instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression psiElement = (JetSimpleNameExpression) diagnostic.getPsiElement();
                    return new ImportClassAndFunFix(psiElement);
                }

                return null;
            }
        };
    }
}
