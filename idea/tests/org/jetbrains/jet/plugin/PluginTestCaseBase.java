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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;

public class PluginTestCaseBase {

    public static final String TEST_DATA_PROJECT_RELATIVE = "/idea/testData";

    private PluginTestCaseBase() {
    }

    public static String getTestDataPathBase() {
        return JetTestCaseBuilder.getHomeDirectory() + TEST_DATA_PROJECT_RELATIVE;
    }

    public static Sdk jdkFromIdeaHome() {
        Sdk sdk = new JavaSdkImpl().createJdk("JDK", "compiler/testData/mockJDK/jre", true);
        SdkModificator modificator = sdk.getSdkModificator();
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(ForTestPackJdkAnnotations.jdkAnnotationsForTests());
        assert file != null;
        modificator.addRoot(JarFileSystem.getInstance().getJarRootForLocalFile(file), AnnotationOrderRootType.getInstance());
        modificator.commitChanges();
        return sdk;
    }
}
