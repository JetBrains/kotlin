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

package org.jetbrains.kotlin.idea;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.RowIcon;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.kotlin.asJava.KotlinLightClassForFacade;
import org.jetbrains.kotlin.idea.caches.resolve.KotlinLightClassForDecompiledDeclaration;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import javax.swing.*;
import java.util.List;

public class JetIconProvider extends IconProvider implements DumbAware {

    public static JetIconProvider INSTANCE = new JetIconProvider();

    @Nullable
    public static KtClassOrObject getMainClass(@NotNull KtFile file) {
        List<KtDeclaration> classes = ContainerUtil.filter(file.getDeclarations(), new Condition<KtDeclaration>() {
            @Override
            public boolean value(KtDeclaration jetDeclaration) {
                return jetDeclaration instanceof KtClassOrObject;
            }
        });
        if (classes.size() == 1) {
            if (StringUtil.getPackageName(file.getName()).equals(classes.get(0).getName())) {
                return (KtClassOrObject) classes.get(0);
            }
        }
        return null;
    }

    @Override
    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        if (psiElement instanceof KtFile) {
            KtFile file = (KtFile) psiElement;
            KtClassOrObject mainClass = getMainClass(file);
            return mainClass != null && file.getDeclarations().size() == 1 ? getIcon(mainClass, flags) : KotlinIcons.FILE;
        }

        Icon result = getBaseIcon(psiElement);
        if ((flags & Iconable.ICON_FLAG_VISIBILITY) > 0 && psiElement instanceof KtModifierListOwner) {
            KtModifierList list = ((KtModifierListOwner) psiElement).getModifierList();
            result = createRowIcon(result, getVisibilityIcon(list));
        }
        return result;
    }

    private static RowIcon createRowIcon(Icon baseIcon, Icon visibilityIcon) {
        RowIcon rowIcon = new RowIcon(2);
        rowIcon.setIcon(baseIcon, 0);
        rowIcon.setIcon(visibilityIcon, 1);
        return rowIcon;
    }

    public static Icon getVisibilityIcon(@Nullable KtModifierList list) {
        if (list != null) {
            if (list.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                return PlatformIcons.PRIVATE_ICON;
            }
            if (list.hasModifier(KtTokens.PROTECTED_KEYWORD)) {
                return PlatformIcons.PROTECTED_ICON;
            }
            if (list.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
                return PlatformIcons.PACKAGE_LOCAL_ICON;
            }
        }

        return PlatformIcons.PUBLIC_ICON;
    }

    public static Icon getBaseIcon(PsiElement psiElement) {
        if (psiElement instanceof KtPackageDirective) {
            return PlatformIcons.PACKAGE_ICON;
        }

        if (psiElement instanceof KotlinLightClassForFacade) {
            return KotlinIcons.FILE;
        }

        if (psiElement instanceof KotlinLightClassForDecompiledDeclaration) {
            KtClassOrObject origin = ((KotlinLightClassForDecompiledDeclaration) psiElement).getOrigin();
            if (origin != null) {
                psiElement = origin;
            }
            else {
                //TODO (light classes for decompiled files): correct presentation
                return KotlinIcons.CLASS;
            }
        }

        if (psiElement instanceof KotlinLightClassForExplicitDeclaration) {
            psiElement = psiElement.getNavigationElement();
        }

        if (psiElement instanceof KtNamedFunction) {
            if (((KtFunction) psiElement).getReceiverTypeReference() != null) {
                return KotlinIcons.EXTENSION_FUNCTION;
            }

            if (PsiTreeUtil.getParentOfType(psiElement, KtNamedDeclaration.class) instanceof KtClass) {
                if (KtPsiUtil.isAbstract((KtFunction) psiElement)) {
                    return PlatformIcons.ABSTRACT_METHOD_ICON;
                }
                else {
                    return PlatformIcons.METHOD_ICON;
                }
            }
            else {
                return KotlinIcons.FUNCTION;
            }
        }

        if (psiElement instanceof KtFunctionLiteral) return KotlinIcons.LAMBDA;

        if (psiElement instanceof KtClass) {
            KtClass ktClass = (KtClass) psiElement;
            if (ktClass.isInterface()) {
                return KotlinIcons.TRAIT;
            }

            Icon icon = ktClass.isEnum() ? KotlinIcons.ENUM : KotlinIcons.CLASS;
            if (ktClass instanceof KtEnumEntry) {
                KtEnumEntry enumEntry = (KtEnumEntry) ktClass;
                if (enumEntry.getPrimaryConstructorParameterList() == null) {
                    icon = KotlinIcons.ENUM;
                }
            }
            return icon;
        }
        if (psiElement instanceof KtObjectDeclaration) {
            return KotlinIcons.OBJECT;
        }
        if (psiElement instanceof KtParameter) {
            KtParameter parameter = (KtParameter) psiElement;
            if (KtPsiUtil.getClassIfParameterIsProperty(parameter) != null) {
                return parameter.isMutable() ? KotlinIcons.FIELD_VAR : KotlinIcons.FIELD_VAL;
            }

            return KotlinIcons.PARAMETER;
        }
        if (psiElement instanceof KtProperty) {
            KtProperty property = (KtProperty) psiElement;
            return property.isVar() ? KotlinIcons.FIELD_VAR : KotlinIcons.FIELD_VAL;
        }

        return null;
    }
}
