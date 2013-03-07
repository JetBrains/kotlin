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

package org.jetbrains.jet.jvm.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/*
   LoadJavaTestGenerated should be used instead if possible.
 */
public final class LoadJavaCustomTest extends AbstractLoadJavaTest {
    @NotNull
    private static final String PATH = "compiler/testData/loadJavaCustom";

    public void testPackageLocalVisibility() throws Exception {
        String dir = PATH + "/packageLocalVisibility/simple/";
        String javaDir = dir + "/java";
        doTestCompiledJava(new File(dir + "/expected.txt"),
                           javaDir + "/test/JFrame.java",
                           javaDir + "/awt/Frame.java");
    }
}
