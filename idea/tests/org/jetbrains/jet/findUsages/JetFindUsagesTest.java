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

package org.jetbrains.jet.findUsages;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.rules.UsageFilteringRule;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.findUsages.JetImportFilteringRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class JetFindUsagesTest extends LightCodeInsightFixtureTestCase {
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

    public void testFindClassUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findClassUsages/Server.kt", "findClassUsages/Client.java"),
                JetClass.class,
                Lists.newArrayList("(4: 13) private Server myServer;"));
    }

    public void testFindUsagesUnresolvedAnnotation() {
        doTestWithoutFiltering(
                Lists.newArrayList("unresolvedAnnotation/Server.kt", "unresolvedAnnotation/Client.java"),
                JetClass.class,
                Lists.newArrayList("(1: 29) public class Client extends Foo {}"));
    }

    public void testFindMethodUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findMethodUsages/Server.kt", "findMethodUsages/Client.java"),
                JetFunction.class,
                Lists.newArrayList("(6: 16) server.processRequest();"));
    }

    public void testFindWithFilteringImports() {
        doTest(Lists.newArrayList("findWithFilteringImports/Test.kt", "findWithFilteringImports/Usages.kt"),
               JetClass.class,
               2,
               Lists.newArrayList(new JetImportFilteringRule()),
               Lists.newArrayList("(5: 9) val a = Test()"));
    }

    private <T extends PsiElement> void doTestWithoutFiltering(Collection<String> files, Class<T> caretElementClass, Collection<String> expectedUsages) {
        doTest(files, caretElementClass, expectedUsages.size(), Collections.<UsageFilteringRule>emptyList(), expectedUsages);
    }

    private  <T extends PsiElement> void doTest(
            Collection<String> files,
            Class<T> caretElementClass,
            int numberOfNotFilteredUsages,
            final Collection<? extends UsageFilteringRule> filters,
            Collection<String> expectedUsages
    ) {
        myFixture.configureByFiles(ArrayUtil.toObjectArray(files, String.class));
        T caretElement = PsiTreeUtil.getParentOfType(myFixture.getElementAtCaret(), caretElementClass, false);
        assertNotNull(String.format("Element with type '%s' wasn't found at caret position", caretElementClass), caretElement);

        Collection<UsageInfo> usageInfos = myFixture.findUsages(caretElement);

        assertEquals(numberOfNotFilteredUsages, usageInfos.size());

        Collection<UsageInfo2UsageAdapter> filteredUsages =
                Collections2.filter(
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

        Function<UsageInfo2UsageAdapter, String> convertToString = new Function<UsageInfo2UsageAdapter, String>() {
            @Override
            public String apply(@Nullable UsageInfo2UsageAdapter usageAdapter) {
                assert usageAdapter != null;
                return Joiner.on("").join(Arrays.asList(usageAdapter.getPresentation().getText()));
            }
        };

        Collection<String> finalUsages = Ordering.natural().sortedCopy(Collections2.transform(filteredUsages, convertToString));
        assertOrderedEquals(finalUsages, Ordering.natural().sortedCopy(expectedUsages));
    }
}
