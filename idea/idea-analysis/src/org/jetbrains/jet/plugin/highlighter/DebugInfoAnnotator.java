/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.checkers.DebugInfoUtil;
import org.jetbrains.jet.lang.psi.JetCodeFragment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.ProjectRootsUtil;
import org.jetbrains.jet.plugin.actions.internal.KotlinInternalMode;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.configuration.JetModuleTypeManager;

/**
 * Quick showing possible problems with Kotlin internals in IDEA with tooltips
 */
public class DebugInfoAnnotator implements Annotator {

    public static boolean isDebugInfoEnabled() {
        return KotlinInternalMode.OBJECT$.getEnabled();
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (!isDebugInfoEnabled() ||
                !ProjectRootsUtil.isInSource(element) ||
                JetModuleTypeManager.getInstance().isKtFileInGradleProjectInWrongFolder(element)) {
            return;
        }

        if (element instanceof JetFile && !(element instanceof JetCodeFragment)) {
            JetFile file = (JetFile) element;
            try {
                BindingContext bindingContext = ResolvePackage.getBindingContext(file);
                DebugInfoUtil.markDebugAnnotations(file, bindingContext, new DebugInfoUtil.DebugInfoReporter() {
                    @Override
                    public void reportElementWithErrorType(@NotNull JetReferenceExpression expression) {
                        holder.createErrorAnnotation(expression, "[DEBUG] Resolved to error element")
                                .setTextAttributes(JetHighlightingColors.RESOLVED_TO_ERROR);
                    }

                    @Override
                    public void reportMissingUnresolved(@NotNull JetReferenceExpression expression) {
                        holder.createErrorAnnotation(expression,
                                                     "[DEBUG] Reference is not resolved to anything, but is not marked unresolved")
                                .setTextAttributes(JetHighlightingColors.DEBUG_INFO);
                    }

                    @Override
                    public void reportUnresolvedWithTarget(@NotNull JetReferenceExpression expression, @NotNull String target) {
                        holder.createErrorAnnotation(expression, "[DEBUG] Reference marked as unresolved is actually resolved to " + target)
                                .setTextAttributes(JetHighlightingColors.DEBUG_INFO);
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
