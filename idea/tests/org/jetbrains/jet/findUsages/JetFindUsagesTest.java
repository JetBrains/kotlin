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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetObjectDeclarationName;
import org.jetbrains.jet.lang.psi.JetProperty;
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

    public void testFindClassJavaUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findClassUsages/javaUsages/Server.kt", "findClassUsages/javaUsages/Client.java"),
                JetClass.class,
                Lists.newArrayList("Field declaration (4: 13) private Server myServer;"));
    }

    public void testFindClassKotlinUsages1() {
        doTestWithoutFiltering(
                Lists.newArrayList("findClassUsages/kotlinUsages1/Server.kt", "findClassUsages/kotlinUsages1/Client.kt"),
                JetClass.class,
                Lists.newArrayList(
                        "Class/object property type (6: 21) var nextServer: Server? = null",
                        "Function return types (14: 26) fun getNextServer(): Server? {",
                        "Function return types (32: 21) fun Any.asServer(): Server? {",
                        "Import directive (3: 15) import server.Server",
                        "Local variable type (10: 21) val server: Server = s",
                        "Nested class/object (7: 16) val name = Server.NAME",
                        "Parameter type (24: 19) fun Client.bar(s: Server) {",
                        "Parameter type (9: 16) fun foo(s: Server) {",
                        "Super type qualifier (19: 15) super<Server>.work()",
                        "Supertype (5: 15) class Client: Server {",
                        "Target type of 'as' operation (33: 40) return if (this is Server) this as Server else null",
                        "Target type of 'is' operation (33: 24) return if (this is Server) this as Server else null"
                ));
    }

    public void testFindClassKotlinUsages2() {
        doTestWithoutFiltering(
                Lists.newArrayList("findClassUsages/kotlinUsages2/Server.kt", "findClassUsages/kotlinUsages2/Client.kt"),
                JetClass.class,
                Lists.newArrayList(
                        "Annotation (5: 1) X(\"\")",
                        "Import directive (3: 15) import server.X"
                ));
    }

    public void testFindClassKotlinUsages3() {
        doTestWithoutFiltering(
                Lists.newArrayList("findClassUsages/kotlinUsages3/Server.kt", "findClassUsages/kotlinUsages3/Client.kt"),
                JetClass.class,
                Lists.newArrayList(
                        "Import directive (3: 15) import server.Data",
                        "Type argument (9: 16) val c = Client<Data>()",
                        "Type constraint (5: 17) class Client<T: Data, X> where X: Data {",
                        "Type constraint (5: 35) class Client<T: Data, X> where X: Data {"
                ));
    }

    public void testFindUsagesUnresolvedAnnotation() {
        doTestWithoutFiltering(
                Lists.newArrayList("unresolvedAnnotation/Server.kt", "unresolvedAnnotation/Client.java"),
                JetClass.class,
                Lists.newArrayList("Usage in extends/implements clause (1: 29) public class Client extends Foo {}"));
    }

    public void testFindMethodJavaUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findMethodUsages/javaUsages/Server.kt", "findMethodUsages/javaUsages/Client.java"),
                JetFunction.class,
                Lists.newArrayList("Unclassified usage (6: 16) server.processRequest();"));
    }

    public void testFindMethodKotlinUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findMethodUsages/kotlinUsages/Server.kt", "findMethodUsages/kotlinUsages/Client.kt"),
                JetFunction.class,
                Lists.newArrayList(
                        "Callable reference (6: 23) val methodRef = ::processRequest()",
                        "Function call (10: 9) processRequest()",
                        "Import directive (3: 15) import server.processRequest"
                ));
    }

    public void testFindPropertyJavaUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findPropertyUsages/javaUsages/Server.kt", "findPropertyUsages/javaUsages/Client.java"),
                JetProperty.class,
                Lists.newArrayList(
                        "Unclassified usage (7: 40) println(\"foo = \" + O.instance$.getFoo())"
                ));
    }

    public void testFindPropertyKotlinUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findPropertyUsages/kotlinUsages/Server.kt", "findPropertyUsages/kotlinUsages/Client.kt"),
                JetProperty.class,
                Lists.newArrayList(
                        "Import directive (3: 15) import server.foo",
                        "Receiver (8: 35) println(\"length: ${server.foo.length()}\")",
                        "Selector (7: 33) println(\"foo = ${server.foo}\")"
                ));
    }

    public void testFindObjectJavaUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findObjectUsages/javaUsages/Server.kt", "findObjectUsages/javaUsages/Client.java"),
                JetObjectDeclarationName.class,
                Lists.newArrayList(
                        "Class static member access (7: 28) println(\"foo = \" + O.instance$.getFoo())",
                        "Usage in import (3: 15) import server.O"
                ));
    }

    public void testFindObjectKotlinUsages() {
        doTestWithoutFiltering(
                Lists.newArrayList("findObjectUsages/kotlinUsages/Server.kt", "findObjectUsages/kotlinUsages/Client.kt"),
                JetObjectDeclarationName.class,
                Lists.newArrayList(
                        "Import directive (3: 15) import server.O",
                        "Receiver (7: 26) println(\"foo = ${O.foo}\")",
                        "Value read (8: 19) val obj = O"
                ));
    }

    public void testFindWithFilteringImports() {
        doTest(Lists.newArrayList("findWithFilteringImports/Test.kt", "findWithFilteringImports/Usages.kt"),
               JetClass.class,
               2,
               Lists.newArrayList(new JetImportFilteringRule()),
               Lists.newArrayList("Instantiation (5: 9) val a = Test()"));
    }

    private <T extends PsiElement> void doTestWithoutFiltering(
            Collection<String> files,
            Class<T> caretElementClass,
            Collection<String> expectedUsages
    ) {
        doTest(files, caretElementClass, expectedUsages.size(), Collections.<UsageFilteringRule>emptyList(), expectedUsages);
    }

    private <T extends PsiElement> void doTest(
            Collection<String> files,
            Class<T> caretElementClass,
            int numberOfNotFilteredUsages,
            Collection<? extends UsageFilteringRule> filters,
            Collection<String> expectedUsages
    ) {
        myFixture.configureByFiles(ArrayUtil.toObjectArray(files, String.class));
        T caretElement = PsiTreeUtil.getParentOfType(myFixture.getElementAtCaret(), caretElementClass, false);
        assertNotNull(String.format("Element with type '%s' wasn't found at caret position", caretElementClass), caretElement);

        Collection<UsageInfo> usageInfos = myFixture.findUsages(caretElement);

        assertEquals(numberOfNotFilteredUsages, usageInfos.size());

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
        assertOrderedEquals(finalUsages, Ordering.natural().sortedCopy(expectedUsages));
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
