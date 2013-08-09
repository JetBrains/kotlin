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

package org.jetbrains.jet.findUsages;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.usages.rules.UsageFilteringRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetObjectDeclarationName;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.findUsages.JetImportFilteringRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractJetFindUsagesTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/findUsages");
    }

    public void testFindClassJavaUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, true, JetClass.class);
    }

    public void testFindClassKotlinUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, false, JetClass.class);
    }

    public void testFindUsagesUnresolvedAnnotation(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, true, JetClass.class);
    }

    public void testFindMethodJavaUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, true, JetFunction.class);
    }

    public void testFindMethodKotlinUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, false, JetFunction.class);
    }

    public void testFindPropertyJavaUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, true, JetProperty.class);
    }

    public void testFindPropertyKotlinUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, false, JetProperty.class);
    }

    public void testFindObjectJavaUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, true, JetObjectDeclarationName.class);
    }

    public void testFindObjectKotlinUsages(@NotNull String path) throws Exception {
        doTestWithoutFiltering(path, false, JetObjectDeclarationName.class);
    }

    public void testFindWithFilteringImports(@NotNull String path) throws Exception {
        doTest(path, false, JetClass.class, Lists.newArrayList(new JetImportFilteringRule()));
    }

    private <T extends PsiElement> void doTestWithoutFiltering(
            String path,
            boolean searchInJava,
            Class<T> caretElementClass
    ) throws Exception {
        doTest(path, searchInJava, caretElementClass, Collections.<UsageFilteringRule>emptyList());
    }

    private <T extends PsiElement> void doTest(
            String path,
            boolean searchInJava,
            Class<T> caretElementClass,
            Collection<? extends UsageFilteringRule> filters
    ) throws IOException {
        String rootPath = path.substring(0, path.lastIndexOf("/") + 1);

        myFixture.configureByFiles(path, rootPath + "Client." + (searchInJava ? "java" : "kt"));
        T caretElement = PsiTreeUtil.getParentOfType(myFixture.getElementAtCaret(), caretElementClass, false);
        assertNotNull(String.format("Element with type '%s' wasn't found at caret position", caretElementClass), caretElement);

        Collection<UsageInfo> usageInfos = myFixture.findUsages(caretElement);

        Collection<UsageInfo2UsageAdapter> filteredUsages = getUsageAdapters(filters, usageInfos);

        Function<UsageInfo2UsageAdapter, String> convertToString = new Function<UsageInfo2UsageAdapter, String>() {
            @Override
            public String apply(@Nullable UsageInfo2UsageAdapter usageAdapter) {
                assert usageAdapter != null;
                return getUsageType(usageAdapter.getElement())
                       + " " + Joiner.on("").join(Arrays.asList(usageAdapter.getPresentation().getText()));
            }
        };

        Collection<String> finalUsages = Ordering.natural().sortedCopy(Collections2.transform(filteredUsages, convertToString));
        String expectedText = FileUtil.loadFile(new File(rootPath + "results.txt"));
        assertOrderedEquals(finalUsages, Ordering.natural().sortedCopy(StringUtil.split(expectedText, "\n")));
    }

    private static Collection<UsageInfo2UsageAdapter> getUsageAdapters(
            final Collection<? extends UsageFilteringRule> filters,
            Collection<UsageInfo> usageInfos
    ) {
        return Collections2.filter(
                Collections2.transform(usageInfos, new Function<UsageInfo, UsageInfo2UsageAdapter>() {
                    @Override
                    public UsageInfo2UsageAdapter apply(@Nullable UsageInfo usageInfo) {
                        assert (usageInfo != null);

                        UsageInfo2UsageAdapter usageAdapter = new UsageInfo2UsageAdapter(usageInfo);
                        for (UsageFilteringRule filter : filters) {
                            if (!filter.isVisible(usageAdapter)) {
                                return null;
                            }
                        }

                        return usageAdapter;
                    }
                }),
                Predicates.notNull());
    }

    @Nullable
    private static UsageType getUsageType(PsiElement element) {
        if (element == null) return null;

        if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) {
            return UsageType.COMMENT_USAGE;
        }

        UsageTypeProvider[] providers = Extensions.getExtensions(UsageTypeProvider.EP_NAME);
        for (UsageTypeProvider provider : providers) {
            UsageType usageType = provider.getUsageType(element);
            if (usageType != null) {
                return usageType;
            }
        }

        return UsageType.UNCLASSIFIED;
    }
}
