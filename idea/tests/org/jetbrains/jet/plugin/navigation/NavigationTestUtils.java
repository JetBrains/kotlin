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

package org.jetbrains.jet.plugin.navigation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.ide.util.gotoByName.GotoSymbolModel2;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.testing.ReferenceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class NavigationTestUtils {
    private NavigationTestUtils() {
    }

    public static GotoTargetHandler.GotoData invokeGotoImplementations(Editor editor, PsiFile psiFile) {
        return new GotoImplementationHandler().getSourceAndTargetElements(editor, psiFile);
    }

    public static void assertGotoImplementations(Editor editor, GotoTargetHandler.GotoData gotoData) {
        // Get expected references from the tested document
        List<String> expectedReferences = InTextDirectivesUtils.findListWithPrefixes(editor.getDocument().getText(), "// REF:");
        Collections.sort(expectedReferences);

        if (gotoData != null) {
            // Transform given reference result to strings
            List<String> psiElements = Lists.transform(Arrays.asList(gotoData.targets), new Function<PsiElement, String>() {
                @Override
                public String apply(@Nullable PsiElement element) {
                    Assert.assertNotNull(element);
                    return ReferenceUtils.renderAsGotoImplementation(element);
                }
            });

            // Compare
            UsefulTestCase.assertOrderedEquals(Ordering.natural().sortedCopy(psiElements), expectedReferences);
        }
        else {
            UsefulTestCase.assertEmpty(expectedReferences);
        }
    }

    public static void assertGotoSymbol(@NotNull Project project, @NotNull Editor editor) {

        List<String> searchTextList = InTextDirectivesUtils.findListWithPrefixes(editor.getDocument().getText(), "// SEARCH_TEXT:");
        Assert.assertFalse("There's no search text in test data file given. Use '// SEARCH_TEXT:' directive",
                           searchTextList.isEmpty());

        List<String> expectedReferences = InTextDirectivesUtils.findListWithPrefixes(editor.getDocument().getText(), "// REF:");

        String searchText = searchTextList.get(0);

        List<Object> elementsByName = new ArrayList<Object>();

        GotoSymbolModel2 model = new GotoSymbolModel2(project);
        String[] names = model.getNames(false);
        for (String name : names) {
            if (name != null && name.startsWith(searchText)) {
                elementsByName.addAll(Arrays.asList(model.getElementsByName(name, false, name + "*", new ProgressIndicatorBase())));
            }
        }

        List<String> renderedElements = Lists.transform(elementsByName, new Function<Object, String>() {
            @Override
            public String apply(@Nullable Object element) {
                Assert.assertNotNull(element);
                Assert.assertTrue(element instanceof PsiElement);
                return ReferenceUtils.renderAsGotoImplementation((PsiElement) element);
            }
        });

        UsefulTestCase.assertOrderedEquals(Ordering.natural().sortedCopy(renderedElements), expectedReferences);
    }
}
