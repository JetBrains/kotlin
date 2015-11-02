/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.DebugInfoUtil;
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.psi.KtCodeFragment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.resolve.BindingContext;

/**
 * Quick showing possible problems with Kotlin internals in IDEA with tooltips
 */
public class DebugInfoAnnotator implements Annotator {

    public static boolean isDebugInfoEnabled() {
        return KotlinInternalMode.Instance.getEnabled();
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (!isDebugInfoEnabled() || !ProjectRootsUtil.isInProjectOrLibSource(element)) {
            return;
        }

        if (element instanceof KtFile && !(element instanceof KtCodeFragment)) {
            KtFile file = (KtFile) element;
            try {
                BindingContext bindingContext = ResolutionUtils.analyzeFully(file);
                DebugInfoUtil.markDebugAnnotations(file, bindingContext, new DebugInfoUtil.DebugInfoReporter() {
                    @Override
                    public void reportElementWithErrorType(@NotNull KtReferenceExpression expression) {
                        holder.createErrorAnnotation(expression, "[DEBUG] Resolved to error element")
                                .setTextAttributes(KotlinHighlightingColors.RESOLVED_TO_ERROR);
                    }

                    @Override
                    public void reportMissingUnresolved(@NotNull KtReferenceExpression expression) {
                        holder.createErrorAnnotation(expression,
                                                     "[DEBUG] Reference is not resolved to anything, but is not marked unresolved")
                                .setTextAttributes(KotlinHighlightingColors.DEBUG_INFO);
                    }

                    @Override
                    public void reportUnresolvedWithTarget(@NotNull KtReferenceExpression expression, @NotNull String target) {
                        holder.createErrorAnnotation(expression, "[DEBUG] Reference marked as unresolved is actually resolved to " + target)
                                .setTextAttributes(KotlinHighlightingColors.DEBUG_INFO);
                    }
                });
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                // TODO
                holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


}
