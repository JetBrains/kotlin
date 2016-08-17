/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.liveTemplates;

import com.intellij.codeInsight.template.EverywhereContextType;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

public abstract class KotlinTemplateContextType extends TemplateContextType {
    private KotlinTemplateContextType(
            @NotNull @NonNls String id,
            @NotNull String presentableName,
            @Nullable java.lang.Class<? extends TemplateContextType> baseContextType
    ) {
        super(id, presentableName, baseContextType);
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        if (!PsiUtilCore.getLanguageAtOffset(file, offset).isKindOf(KotlinLanguage.INSTANCE)) {
            return false;
        }

        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            element = file.findElementAt(offset - 1);
        }

        if (element instanceof PsiWhiteSpace) {
            return false;
        }
        else if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) {
            return isCommentInContext();
        }
        else if (PsiTreeUtil.getParentOfType(element, KtPackageDirective.class) != null
                || PsiTreeUtil.getParentOfType(element, KtImportDirective.class) != null) {
            return false;
        }
        else if (element instanceof LeafPsiElement) {
            IElementType elementType = ((LeafPsiElement) element).getElementType();
            if (elementType == KtTokens.IDENTIFIER) {
                PsiElement parent = element.getParent();
                if (parent instanceof KtReferenceExpression) {
                    PsiElement parentOfParent = parent.getParent();
                    KtQualifiedExpression qualifiedExpression = PsiTreeUtil.getParentOfType(element, KtQualifiedExpression.class);
                    if (qualifiedExpression != null && qualifiedExpression.getSelectorExpression() == parentOfParent) {
                        return false;
                    }
                }
            }
        }

        return element != null && isInContext(element);
    }

    protected boolean isCommentInContext() {
        return false;
    }

    protected abstract boolean isInContext(@NotNull PsiElement element);

    public static class Generic extends KotlinTemplateContextType {
        public Generic() {
            super("KOTLIN", KotlinLanguage.NAME, EverywhereContextType.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return true;
        }

        @Override
        protected boolean isCommentInContext() {
            return true;
        }
    }

    public static class TopLevel extends KotlinTemplateContextType {
        public TopLevel() {
            super("KOTLIN_TOPLEVEL", "Top-level", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement e = element;
            while (e != null) {
                if (e instanceof KtModifierList) {
                    // skip property/function/class or object which is owner of modifier list
                    e = e.getParent();
                    if (e != null) {
                        e = e.getParent();
                    }
                    continue;
                }
                if (e instanceof KtProperty || e instanceof KtNamedFunction || e instanceof KtClassOrObject) {
                    return false;
                }
                if (e instanceof KtScriptInitializer) {
                    return false;
                }
                e = e.getParent();
            }
            return true;
        }
    }

    public static class ObjectDeclaration extends KotlinTemplateContextType {
        public ObjectDeclaration() {
            super("KOTLIN_OBJECT_DECLARATION", "Object declaration", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            KtObjectDeclaration objectDeclaration = getParentClassOrObject(element, KtObjectDeclaration.class);
            return objectDeclaration != null && !objectDeclaration.isObjectLiteral();
        }
    }

    public static class Class extends KotlinTemplateContextType {
        public Class() {
            super("KOTLIN_CLASS", "Class", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return getParentClassOrObject(element, KtClassOrObject.class) != null;
        }
    }

    public static class Statement extends KotlinTemplateContextType {
        public Statement() {
            super("KOTLIN_STATEMENT", "Statement", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement parentStatement = PsiTreeUtil.findFirstParent(element, new Condition<PsiElement>() {
                @Override
                public boolean value(PsiElement element) {
                    return element instanceof KtExpression && (element.getParent() instanceof KtBlockExpression);
                }
            });

            if (parentStatement == null) return false;

            // We are in the leftmost position in parentStatement
            return element.getTextOffset() == parentStatement.getTextOffset();
        }
    }

    public static class Expression extends KotlinTemplateContextType {
        public Expression() {
            super("KOTLIN_EXPRESSION", "Expression", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return element.getParent() instanceof KtExpression && !(element.getParent() instanceof KtConstantExpression) &&
                   !(element.getParent().getParent() instanceof KtDotQualifiedExpression)
                   && !(element.getParent() instanceof KtParameter);
        }
    }

    public static class Comment extends KotlinTemplateContextType {
        public Comment() {
            super("KOTLIN_COMMENT", "Comment", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return false;
        }

        @Override
        protected boolean isCommentInContext() {
            return true;
        }
    }

    private static <T extends PsiElement> T getParentClassOrObject(@NotNull PsiElement element, @NotNull java.lang.Class<? extends T> klass) {
        PsiElement e = element;
        while (e != null && !klass.isInstance(e)) {
            if (e instanceof KtModifierList) {
                // skip property/function/class or object which is owner of modifier list
                e = e.getParent();
                if (e != null) {
                    e = e.getParent();
                }
                continue;
            }
            if (e instanceof KtProperty || e instanceof KtNamedFunction) {
                return null;
            }
            e = e.getParent();
        }

        //noinspection unchecked
        return (T) e;
    }
}
