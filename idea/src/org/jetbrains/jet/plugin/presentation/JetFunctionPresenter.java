/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.presentation;

import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProvider;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.plugin.JetIconProvider;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import javax.swing.*;

/**
 * @author Nikolay Krasko
 */
public class JetFunctionPresenter implements ItemPresentationProvider<JetNamedFunction> {
    @Override
    public ItemPresentation getPresentation(final JetNamedFunction function) {
        return new ColoredItemPresentation() {
            @Override
            public TextAttributesKey getTextAttributesKey() {
                return null;
            }

            @Override
            public String getPresentableText() {
                return function.getName();
            }

            @Override
            public String getLocationString() {
                return String.format("(in %s)", QualifiedNamesUtil.withoutLastSegment(JetPsiUtil.getFQName(function)));
            }

            @Override
            public Icon getIcon(boolean open) {
                return JetIconProvider.INSTANCE.getIcon(function, 0);
            }
        };
    }
}
