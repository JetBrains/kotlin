/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestFixture
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(JUnit3WithIdeaConfigurationRunner.class)
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
