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

package org.jetbrains.jet.testing;

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.KotlinLightClass;
import org.jetbrains.jet.plugin.presentation.JetLightClassListCellRenderer;

public final class ReferenceUtils {
    private ReferenceUtils() {
    }

    public static String renderAsGotoImplementation(@NotNull PsiElement element) {
        if (element instanceof KotlinLightClass) {
            KotlinLightClass jetLightClass = (KotlinLightClass) element;
            JetLightClassListCellRenderer renderer = new JetLightClassListCellRenderer();
            String elementText = renderer.getElementText(jetLightClass);
            String containerText = JetLightClassListCellRenderer.getContainerTextStatic(jetLightClass);
            return (containerText != null) ? containerText + "." + elementText : elementText;
        }

        Assert.assertTrue(element instanceof NavigationItem);
        ItemPresentation presentation = ((NavigationItem) element).getPresentation();

        if (presentation == null) {
            return element.getText();
        }

        String presentableText = presentation.getPresentableText();
        String locationString = presentation.getLocationString();
        return locationString == null || element instanceof PsiPackage // for PsiPackage, presentableText is FQ name of current package
               ? presentableText
               : locationString + "." + presentableText;
    }
}
