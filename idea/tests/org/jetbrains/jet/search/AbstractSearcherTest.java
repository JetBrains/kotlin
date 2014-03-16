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

package org.jetbrains.jet.search;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSearcherTest extends LightCodeInsightFixtureTestCase {

    protected PsiClass getPsiClass(String className) {
        PsiClass psiClass = JavaPsiFacade.getInstance(getProject()).findClass(className, getGlobalScope());
        assertNotNull("Couldn't find a psiClass: " + className, psiClass);
        return psiClass;
    }

    private GlobalSearchScope getGlobalScope() {
        return GlobalSearchScope.allScope(getProject());
    }

    protected GlobalSearchScope getProjectScope() {
        return GlobalSearchScope.projectScope(getProject());
    }

    protected void checkResult(Query<?> actual) throws IOException {
        List<String> expected = InTextDirectivesUtils.findListWithPrefixes(FileUtil.loadFile(new File(getPathToFile()), true), "// SEARCH: ");
        List<String> actualModified = new ArrayList<String>();
        for (Object member : actual) {
            actualModified.add(member.toString());
        }
        Collections.sort(expected);
        Collections.sort(actualModified);
        assertOrderedEquals(actualModified, expected);
    }

    protected String getPathToFile() {
        return getTestDataPath() + File.separator + getName() + ".kt";
    }

    protected String getFileName() {
        return getName() + ".kt";
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}