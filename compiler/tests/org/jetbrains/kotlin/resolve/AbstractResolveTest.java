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

package org.jetbrains.kotlin.resolve;

import com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.checkers.CheckerTestUtilTest;
import org.jetbrains.kotlin.psi.KtFile;

public abstract class AbstractResolveTest extends ExtensibleResolveTestCase {
    @Override
    protected ExpectedResolveData getExpectedResolveData() {
        final Project project = getProject();

        return new ExpectedResolveData(
                ExpectedResolveDataUtil.prepareDefaultNameToDescriptors(project, getEnvironment()),
                ExpectedResolveDataUtil.prepareDefaultNameToDeclaration(project, getEnvironment())
        ) {
            @Override
            protected KtFile createKtFile(String fileName, String text) {
                return CheckerTestUtilTest.createCheckAndReturnPsiFile(fileName, text, project);
            }
        };
    }
}
