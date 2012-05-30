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

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;

/**
 * @author Stepan Koltsov
 */
public class ScriptGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
    }

    public void testHelloWorld() {
        blackBoxFile("script/helloWorld.ktscript");
    }

    public void testString() {
        blackBoxFile("script/string.ktscript");
    }

    public void testTopLevelFunction() {
        blackBoxFile("script/topLevelFunction.ktscript");
        // TODO: check function is visible as instance field (it is currently not)
    }

    public void testScriptParameter() {
        blackBoxFile("script/parameter.ktscript");
    }

    public void testScriptParameterLong() {
        blackBoxFile("script/parameterLong.ktscript");
    }

    public void testScriptParameterArray() {
        blackBoxFile("script/parameterArray.ktscript");
    }

}
