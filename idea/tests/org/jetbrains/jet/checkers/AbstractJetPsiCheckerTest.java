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

import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.JetWithJdkAndMinimalRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.highlighter.JetPsiChecker;

public abstract class AbstractJetPsiCheckerTest extends JetLightCodeInsightFixtureTestCase {
    public void doTest(@NotNull String filePath) throws Exception {
        myFixture.configureByFile(filePath);
        myFixture.checkHighlighting(true, false, false);
    }

    public void doTestWithInfos(@NotNull String filePath) throws Exception {
        try {
            myFixture.configureByFile(filePath);

            //noinspection unchecked
            myFixture.enableInspections(SpellCheckingInspection.class);

            JetPsiChecker.setNamesHighlightingEnabled(false);
            myFixture.checkHighlighting(true, true, false);
        }
        finally {
            JetPsiChecker.setNamesHighlightingEnabled(true);
        }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndMinimalRuntimeLightProjectDescriptor.INSTANCE;
    }
}
