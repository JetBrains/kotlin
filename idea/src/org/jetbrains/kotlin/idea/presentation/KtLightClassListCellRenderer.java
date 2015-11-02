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

import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.psi.presentation.java.ClassPresentationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClass;

public class KtLightClassListCellRenderer extends PsiElementListCellRenderer<KtLightClass> {
    @Override
    public String getElementText(@NotNull KtLightClass element) {
        return ClassPresentationUtil.getNameForClass(element, false);
    }

    @Override
    protected String getContainerText(KtLightClass element, String name) {
        return getContainerTextStatic(element);
    }

    @Nullable
    public static String getContainerTextStatic(KtLightClass element) {
        return "(" + element.getFqName().parent() + ")";
    }

    @Override
    protected int getIconFlags() {
        return 0;
    }
}
