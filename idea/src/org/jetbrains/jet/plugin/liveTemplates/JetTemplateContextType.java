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

package org.jetbrains.jet.plugin.liveTemplates;

import com.intellij.codeInsight.template.EverywhereContextType;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.plugin.JetLanguage;

public abstract class JetTemplateContextType extends TemplateContextType {
    protected JetTemplateContextType(@NotNull @NonNls String id, @NotNull String presentableName, @Nullable java.lang.Class<? extends TemplateContextType> baseContextType) {
        super(id, presentableName, baseContextType);
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        if (PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(JetLanguage.INSTANCE)) {
            PsiElement element = file.findElementAt(offset);
            if (element instanceof PsiWhiteSpace || element instanceof PsiComment) {
                return false;
            }
            else if (PsiTreeUtil.getParentOfType(element, JetPackageDirective.class) != null
                    || PsiTreeUtil.getParentOfType(element, JetImportDirective.class) != null) {
                return false;
            }
            else if (element instanceof LeafPsiElement) {
                IElementType elementType = ((LeafPsiElement) element).getElementType();
                if (elementType == JetTokens.IDENTIFIER) {
                    if (element.getParent() instanceof JetReferenceExpression) {
                        PsiElement parentOfParent = element.getParent().getParent();
                        JetQualifiedExpression qualifiedExpression = PsiTreeUtil.getParentOfType(element, JetQualifiedExpression.class);
                        if (qualifiedExpression != null && qualifiedExpression.getSelectorExpression() == parentOfParent) {
                            return false;
                        }
                    }
                    else {
                        return false;
                    }
                }
            }
            return element != null && isInContext(element);
        }

        return false;
    }

    protected abstract boolean isInContext(@NotNull PsiElement element);

    public static class Generic extends JetTemplateContextType {
        public Generic() {
            super("KOTLIN", JetLanguage.NAME, EverywhereContextType.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return true;
        }
    }

    public static class TopLevel extends JetTemplateContextType {
        public TopLevel() {
            super("KOTLIN_TOPLEVEL", "Top-level", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement e = element;
            while (e != null) {
                if (e instanceof JetModifierList) {
                    // skip property/function/class or object which is owner of modifier list
                    e = e.getParent();
                    if (e != null) {
                        e = e.getParent();
                    }
                    continue;
                }
                if (e instanceof JetProperty || e instanceof JetNamedFunction
                    || e instanceof JetClassOrObject) {
                    return false;
                }
                e = e.getParent();
            }
            return true;
        }
    }

    public static class Class extends JetTemplateContextType {
        public Class() {
            super("KOTLIN_CLASS", "Class", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement e = element;
            while (e != null && !(e instanceof JetClassOrObject)) {
                if (e instanceof JetModifierList) {
                    // skip property/function/class or object which is owner of modifier list
                    e = e.getParent();
                    if (e != null) {
                        e = e.getParent();
                    }
                    continue;
                }
                if (e instanceof JetProperty || e instanceof JetNamedFunction) {
                    return false;
                }
                e = e.getParent();
            }
            return true;
        }
    }

    public static class Statement extends JetTemplateContextType {
        public Statement() {
            super("KOTLIN_STATEMENT", "Statement", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement parentStatement = PsiTreeUtil.findFirstParent(element, new Condition<PsiElement>() {
                @Override
                public boolean value(PsiElement element) {
                    return element instanceof JetExpression && (element.getParent() instanceof JetBlockExpression);
                }
            });

            if (parentStatement == null) return false;

            // We are in the leftmost position in parentStatement
            return element.getTextOffset() == parentStatement.getTextOffset();
        }
    }

    public static class Expression extends JetTemplateContextType {
        public Expression() {
            super("KOTLIN_EXPRESSION", "Expression", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return element.getParent() instanceof JetExpression && !(element.getParent() instanceof JetConstantExpression) &&
                   !(element.getParent().getParent() instanceof JetDotQualifiedExpression)
                   && !(element.getParent() instanceof JetParameter);
        }
    }
}
