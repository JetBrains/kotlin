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

package org.jetbrains.kotlin.checkers;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.dataFlow.DataFlowInspection;
import com.intellij.codeInspection.nullable.NullableStuffInspection;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.siyeh.ig.bugs.StaticCallOnSubclassInspection;
import com.siyeh.ig.bugs.StaticFieldReferenceOnSubclassInspection;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KotlinAndJavaCheckerTest extends KotlinDaemonAnalyzerTestCase {
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
        File configureFile = new File(getTestDataPath(), getTestName(false) + ".txt");
        if (!configureFile.exists()) return null;

        try {
            return FileUtil.loadFile(configureFile, true);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @Override
    protected LocalInspectionTool[] configureLocalInspectionTools() {
        String configFileText = getConfigFileText();
        if (configFileText == null) return DEFAULT_TOOLS;

        List<String> toolsStrings = InTextDirectivesUtils.findListWithPrefixes(configFileText, "TOOL:");

        return ArrayUtil.toObjectArray(KotlinPackage.map(toolsStrings, new Function1<String, LocalInspectionTool>() {
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
        if (configFileText != null && InTextDirectivesUtils.isDirectiveDefined(configFileText, "// WITH_RUNTIME")) {
            ConfigLibraryUtil.configureKotlinRuntime(module);
        }

        return module;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/kotlinAndJavaChecker/";
    }

    public void testClassObjects() throws Exception {
        doTest();
    }

    public void testNoNotNullOnParameterInOverride() throws Exception {
        doTest();
    }

    public void testTopLevelFunctionInDataFlowInspection() throws Exception {
        doTest();
    }

    public void testUsingKotlinPackageDeclarations() throws Exception {
        doTest();
    }

    public void testAssignKotlinClassToObjectInJava() throws Exception {
        doTest();
    }

    public void testAssignMappedKotlinType() throws Exception {
        doTest();
    }

    public void testUseKotlinSubclassesOfMappedTypes() throws Exception {
        doTest();
    }

    public void testImplementedMethodsFromTraits() throws Exception {
        doTest();
    }

    public void testJvmOverloadsFunctions() throws Exception {
        doTest();
    }

    public void testEnumAutoGeneratedMethods() throws Exception {
        doTest();
    }

    public void testEnumEntriesInSwitch() throws Exception {
        doTest();
    }

    public void testEnumStaticImportInJava() throws Exception {
        doTest();
    }

    public void doTest() throws Exception {
        doTest(true, true, getTestName(false) + ".java", getTestName(false) + ".kt");
    }
}
