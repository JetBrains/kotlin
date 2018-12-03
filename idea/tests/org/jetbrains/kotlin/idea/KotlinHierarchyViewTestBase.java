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

package org.jetbrains.kotlin.idea;

import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestFixture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;

import java.io.File;
import java.io.IOException;

public abstract class KotlinHierarchyViewTestBase extends KotlinLightCodeInsightFixtureTestCase {
    private final HierarchyViewTestFixture hierarchyFixture = new HierarchyViewTestFixture();

    protected void doHierarchyTest(
            @NotNull Computable<? extends HierarchyTreeStructure> treeStructureComputable,
            @NotNull String... fileNames
    ) throws Exception {
        configure(fileNames);
        String expectedStructure = loadExpectedStructure();

        hierarchyFixture.doHierarchyTest(treeStructureComputable.compute(), expectedStructure);
    }

    private void configure(@NotNull String[] fileNames) {
        myFixture.configureByFiles(fileNames);
    }

    @NotNull
    private String loadExpectedStructure() throws IOException {
        String verificationFilePath = getTestDataPath() + "/" + getTestName(false) + "_verification.xml";
        return FileUtil.loadFile(new File(verificationFilePath));
    }
}
