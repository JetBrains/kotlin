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

package org.jetbrains.kotlin.idea.refactoring.memberInfo;

import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiNamedElement;
import com.intellij.ui.ListCellRendererWrapper;

import javax.swing.*;

public class KotlinOrJavaClassCellRenderer extends ListCellRendererWrapper<PsiNamedElement> {
    @Override
    public void customize(JList list, PsiNamedElement value, int index, boolean selected, boolean hasFocus) {
        if (value == null) return;

        setText(MemberInfoPackage.qualifiedClassNameForRendering(value));
        Icon icon = value.getIcon(Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS);
        if (icon != null) {
            setIcon(icon);
        }
    }
}
