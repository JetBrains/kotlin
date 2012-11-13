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

package org.jetbrains.jet.codegen.extension;

import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.codegen.CodegenTestCase;

import java.io.IOException;

public abstract class AbstractExtensionPropertiesTest extends CodegenTestCase {

    private static final String REDUNDANT_PATH_PREFIX = "compiler/testData/codegen";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    protected void doTest(String path) throws IOException {
        String relativePath = path.substring(REDUNDANT_PATH_PREFIX.length());
        blackBoxFile(relativePath);
    }

}
