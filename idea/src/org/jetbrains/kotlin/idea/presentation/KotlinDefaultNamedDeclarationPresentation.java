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

package org.jetbrains.kotlin.idea.presentation;

import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import org.jetbrains.kotlin.idea.KotlinIconProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtPsiUtil;

import javax.swing.*;

public class KotlinDefaultNamedDeclarationPresentation implements ColoredItemPresentation {
    private final KtNamedDeclaration declaration;

    KotlinDefaultNamedDeclarationPresentation(KtNamedDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public TextAttributesKey getTextAttributesKey() {
        if (KtPsiUtil.isDeprecated(declaration)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES;
        }
        return null;
    }

    @Override
    public String getPresentableText() {
        return declaration.getName();
    }

    @Override
    public String getLocationString() {
        FqName name = declaration.getFqName();
        if (name != null) {
            return "(" + name.parent().toString() + ")";
        }

        return null;
    }

    @Override
    public Icon getIcon(boolean unused) {
        return KotlinIconProvider.INSTANCE.getIcon(declaration, Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS);
    }
}
