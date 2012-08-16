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

package org.jetbrains.jet.plugin.navigation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.plugin.presentation.JetLightClassListCellRenderer;
import org.jetbrains.jet.testing.InTextDirectivesUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
final class ImplementationTestUtils {
    private ImplementationTestUtils() {
    }

    public static GotoTargetHandler.GotoData invokeGotoImplementations(Editor editor, PsiFile psiFile) {
        return new GotoImplementationHandler().getSourceAndTargetElements(editor, psiFile);
    }

    public static void assertGotoImplementations(Editor editor, GotoTargetHandler.GotoData gotoData) {
        // Get expected references from the tested document
        List<String> expectedReferences = Arrays.asList(InTextDirectivesUtils.findListWithPrefix("// REF:", editor.getDocument().getText()));
        Collections.sort(expectedReferences);

        if (gotoData != null) {
            // Transform given reference result to strings
            List<String> psiElements = Lists.transform(Arrays.asList(gotoData.targets), new Function<PsiElement, String>() {
                @Override
                public String apply(@Nullable PsiElement element) {
                    Assert.assertNotNull(element);
                    if (element instanceof JetLightClass) {
                        JetLightClass jetLightClass = (JetLightClass) element;
                        JetLightClassListCellRenderer renderer = new JetLightClassListCellRenderer();
                        String elementText = renderer.getElementText(jetLightClass);
                        String containerText = JetLightClassListCellRenderer.getContainerTextStatic(jetLightClass);
                        return (containerText != null) ? containerText + "." + elementText : elementText;
                    }

                    Assert.assertTrue(element instanceof NavigationItem);
                    ItemPresentation presentation = ((NavigationItem) element).getPresentation();

                    Assert.assertNotNull(presentation);

                    String presentableText = presentation.getPresentableText();
                    String locationString = presentation.getLocationString();
                    return locationString != null ? (locationString + "." + presentableText) : presentableText;
                }
            });

            // Compare
            UsefulTestCase.assertOrderedEquals(Ordering.natural().sortedCopy(psiElements), expectedReferences);
        }
        else {
            UsefulTestCase.assertEmpty(expectedReferences);
        }
    }
}
