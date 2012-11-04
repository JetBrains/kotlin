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

package org.jetbrains.jet.plugin;

import com.intellij.ide.IconProvider;
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
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import javax.swing.*;
import java.util.List;

/**
 * @author yole
 */
public class JetIconProvider extends IconProvider {

    public static JetIconProvider INSTANCE = new JetIconProvider();

    @Nullable
    public static JetClassOrObject getMainClass(@NotNull JetFile file) {
        List<JetDeclaration> classes = ContainerUtil.filter(file.getDeclarations(), new Condition<JetDeclaration>() {
            @Override
            public boolean value(JetDeclaration jetDeclaration) {
                return jetDeclaration instanceof JetClassOrObject;
            }
        });
        if (classes.size() == 1) {
            if (StringUtil.getPackageName(file.getName()).equals(classes.get(0).getName())) {
                return (JetClassOrObject) classes.get(0);
            }
        }
        return null;
    }

    @Override
    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        if (psiElement instanceof JetFile) {
            JetFile file = (JetFile) psiElement;
            JetClassOrObject mainClass = getMainClass(file);
            return mainClass != null && file.getDeclarations().size() == 1 ? getIcon(mainClass, flags) : JetIcons.FILE;
        }

        Icon result = getBaseIcon(psiElement);
        if ((flags & Iconable.ICON_FLAG_VISIBILITY) > 0 && psiElement instanceof JetModifierListOwner) {
            JetModifierList list = ((JetModifierListOwner) psiElement).getModifierList();
            if (list != null) {
                RowIcon rowIcon = new RowIcon(2);
                rowIcon.setIcon(result, 0);
                rowIcon.setIcon(getVisibilityIcon(list), 1);
                result = rowIcon;
            }
        }
        return result;
    }

    public static Icon getVisibilityIcon(JetModifierList list) {
        if (list.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
            return PlatformIcons.PRIVATE_ICON;
        }
        if (list.hasModifier(JetTokens.PROTECTED_KEYWORD)) {
            return PlatformIcons.PROTECTED_ICON;
        }
        if (list.hasModifier(JetTokens.PUBLIC_KEYWORD)) {
            return PlatformIcons.PUBLIC_ICON;
        }
        return PlatformIcons.PACKAGE_LOCAL_ICON;
    }

    public static Icon getBaseIcon(PsiElement psiElement) {
        if (psiElement instanceof JetNamespaceHeader) {
            return PlatformIcons.PACKAGE_ICON;
        }
        if (psiElement instanceof JetNamedFunction) {
            if (((JetFunction) psiElement).getReceiverTypeRef() != null) {
                return JetIcons.EXTENSION_FUNCTION;
            }

            if (PsiTreeUtil.getParentOfType(psiElement, JetNamedDeclaration.class) instanceof JetClass) {
                if (JetPsiUtil.isAbstract((JetFunction) psiElement)) {
                    return PlatformIcons.ABSTRACT_METHOD_ICON;
                }
                else {
                    return PlatformIcons.METHOD_ICON;
                }
            }
            else {
                return JetIcons.FUNCTION;
            }
        }
        if (psiElement instanceof JetClass) {
            JetClass jetClass = (JetClass) psiElement;
            if (jetClass.isTrait()) {
                return JetIcons.TRAIT;
            }

            Icon icon = jetClass.hasModifier(JetTokens.ENUM_KEYWORD) ? PlatformIcons.ENUM_ICON : JetIcons.CLASS;
            if (jetClass instanceof JetEnumEntry) {
                JetEnumEntry enumEntry = (JetEnumEntry) jetClass;
                if (enumEntry.getPrimaryConstructorParameterList() == null) {
                    icon = PlatformIcons.ENUM_ICON;
                }
            }
            return icon;
        }
        if (psiElement instanceof JetObjectDeclaration || psiElement instanceof JetClassObject) {
            return JetIcons.OBJECT;
        }
        if (psiElement instanceof JetParameter) {
            JetParameter parameter = (JetParameter) psiElement;
            if (parameter.getValOrVarNode() != null) {
                JetParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, JetParameterList.class);
                if (parameterList != null && parameterList.getParent() instanceof JetClass) {
                    return parameter.isMutable() ? JetIcons.FIELD_VAR : JetIcons.FIELD_VAL;
                }
            }
            return JetIcons.PARAMETER;
        }
        if (psiElement instanceof JetProperty) {
            JetProperty property = (JetProperty) psiElement;
            return property.isVar() ? JetIcons.FIELD_VAR : JetIcons.FIELD_VAL;
        }

        if (psiElement instanceof JetLightClass) {
            return JetIcons.CLASS;
        }

        return null;
    }
}
