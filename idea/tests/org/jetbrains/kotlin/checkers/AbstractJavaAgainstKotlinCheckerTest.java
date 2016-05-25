/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.checkers;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.dataFlow.DataFlowInspection;
import com.intellij.codeInspection.nullable.NullableStuffInspection;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.util.ArrayUtil;
import com.siyeh.ig.bugs.StaticCallOnSubclassInspection;
import com.siyeh.ig.bugs.StaticFieldReferenceOnSubclassInspection;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class AbstractJavaAgainstKotlinCheckerTest extends KotlinDaemonAnalyzerTestCase {

    private static final LocalInspectionTool[] DEFAULT_TOOLS = new LocalInspectionTool[] {
            new StaticCallOnSubclassInspection(),
            new StaticFieldReferenceOnSubclassInspection(),
            new NullableStuffInspection()
    };

    private static LocalInspectionTool mapStringToTool(String toolString) {
        if ("StaticCallOnSubclassInspection".equals(toolString))
            return new StaticCallOnSubclassInspection();
        if ("StaticFieldReferenceOnSubclassInspection".equals(toolString))
            return new StaticFieldReferenceOnSubclassInspection();
        if ("NullableStuffInspection".equals(toolString))
            return new NullableStuffInspection();
        if ("DataFlowInspection".equals(toolString))
            return new DataFlowInspection();

        throw new IllegalArgumentException("Can't find inspection tool with identifier: " + toolString);
    }

    @Nullable
    protected String getConfigFileText() {
        File configureFile = new File(PluginTestCaseBase.getTestDataPathBase() + "/kotlinAndJavaChecker/javaAgainstKotlin/" + getTestName(false) + ".txt");
        if (!configureFile.exists()) return null;

        try {
            return FileUtil.loadFile(configureFile, true);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @Override
    protected LocalInspectionTool[] configureLocalInspectionTools() {
        String configFileText = getConfigFileText();
        if (configFileText == null) return DEFAULT_TOOLS;

        List<String> toolsStrings = InTextDirectivesUtils.findListWithPrefixes(configFileText, "TOOL:");

        return ArrayUtil.toObjectArray(CollectionsKt.map(toolsStrings, new Function1<String, LocalInspectionTool>() {
            @Override
            public LocalInspectionTool invoke(String toolString) {
                return mapStringToTool(toolString);
            }
        }), LocalInspectionTool.class);
    }

    @Override
    protected Module createMainModule() throws IOException {
        Module module = super.createMainModule();

        String configFileText = getConfigFileText();
        if (configFileText == null) {
            return module;
        }

        if (InTextDirectivesUtils.isDirectiveDefined(configFileText, "// WITH_RUNTIME")) {
            ConfigLibraryUtil.configureKotlinRuntime(module);
        }

        List<String> languageLevelLines = InTextDirectivesUtils.findLinesWithPrefixesRemoved(configFileText, "// LANGUAGE_LEVEL");
        if (languageLevelLines.size() > 1) {
            throw new AssertionError("Language level specified multiple times: " + languageLevelLines);
        }
        if (languageLevelLines.size() == 1) {
            LanguageLevel level = LanguageLevel.parse(languageLevelLines.iterator().next());
            if (level != null) {
                IdeaTestUtil.setModuleLanguageLevel(module, level);
            }
        }

        return module;
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }

    @Override
    protected String getTestDataPath() {
        return KotlinTestUtils.getHomeDirectory() + "/";
    }
}
