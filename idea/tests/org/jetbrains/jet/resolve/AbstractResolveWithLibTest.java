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

package org.jetbrains.jet.resolve;

import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor;

import static com.intellij.openapi.util.text.StringUtil.startsWithIgnoreCase;

public abstract class AbstractResolveWithLibTest extends AbstractResolveBaseTest {

    private static final String TEST_DATA_PATH = "idea/testData/resolve/referenceWithLib";
    private static final String ALL_FILES_PRESENT_PREFIX = "allFilesPresentIn";

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        String testName = getTestName(true);
        if (startsWithIgnoreCase(testName, ALL_FILES_PRESENT_PREFIX)) {
            return null;
        }
        return new JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/" + testName + "Src", false);
    }
}
