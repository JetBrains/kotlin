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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.intentions;

import com.google.common.base.Predicate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.AbstractCodeTransformationIntention;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.IfWhenUtils;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.core.Transformer;

public class WhenToIfIntention extends AbstractCodeTransformationIntention<Transformer> {
    private static final Transformer TRANSFORMER = new Transformer() {
        @NotNull
        @Override
        public String getKey() {
            return "when.to.if";
        }

        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor) {
            IfWhenUtils.transformWhenToIf((JetWhenExpression) element);
        }
    };

    private static final Predicate<PsiElement> IS_APPLICABLE = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return (input instanceof JetWhenExpression) && IfWhenUtils.checkWhenToIf((JetWhenExpression) input);
        }
    };

    public WhenToIfIntention() {
        super(TRANSFORMER, IS_APPLICABLE);
    }
}
