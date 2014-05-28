/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.checkers;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.nullable.NullableStuffInspection;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.siyeh.ig.bugs.StaticCallOnSubclassInspection;
import com.siyeh.ig.bugs.StaticFieldReferenceOnSubclassInspection;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

public class KotlinAndJavaCheckerTest extends KotlinDaemonAnalyzerTestCase {
    @Override
    protected LocalInspectionTool[] configureLocalInspectionTools() {
        return new LocalInspectionTool[] {
                new StaticCallOnSubclassInspection(),
                new StaticFieldReferenceOnSubclassInspection(),
                new NullableStuffInspection()
        };
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData/kotlinAndJavaChecker/";
    }

    public void testName() throws Exception {
        doTest(false, false, "ClassObjects.java", "ClassObjects.kt");
    }

    public void testNoNotNullOnParameterInOverride() throws Exception {
        doTest(true, true, "NoNotNullOnParameterInOverride.java", "NoNotNullOnParameterInOverride.kt");
    }

    public void testUsingKotlinPackageDeclarations() throws Exception {
        doTest(true, true, "UsingKotlinPackageDeclarations.java", "UsingKotlinPackageDeclarations.kt");
    }

    public void testAssignKotlinClassToObjectInJava() throws Exception {
        doTest(true, true, "AssignKotlinClassToObjectInJava.java", "AssignKotlinClassToObjectInJava.kt");
    }

    public void testAssignMappedKotlinType() throws Exception {
        doTest(true, true, "AssignMappedKotlinType.java", "AssignMappedKotlinType.kt");
    }

    public void testUseKotlinSubclassesOfMappedTypes() throws Exception {
        doTest(true, true, "UseKotlinSubclassesOfMappedTypes.java", "UseKotlinSubclassesOfMappedTypes.kt");
    }
}
