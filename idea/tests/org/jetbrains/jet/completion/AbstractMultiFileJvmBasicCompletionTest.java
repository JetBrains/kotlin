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

package org.jetbrains.jet.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.completion.util.UtilPackage;
import org.jetbrains.jet.plugin.KotlinCompletionTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.jet.plugin.stubs.AstAccessControl;

public abstract class AbstractMultiFileJvmBasicCompletionTest extends KotlinCompletionTestCase {
    protected void doTest(@NotNull String testPath) throws Exception {
        configureByFile(getTestName(false) + ".kt", "");
        boolean shouldFail = testPath.contains("NoSpecifiedType");
        AstAccessControl.INSTANCE$.testWithControlledAccessToAst(
                shouldFail, getFile().getVirtualFile(), getProject(), getTestRootDisposable(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        UtilPackage.testCompletion(getFile().getText(), TargetPlatform.JVM, new Function1<Integer, LookupElement[]>() {
                            @Override
                            public LookupElement[] invoke(Integer invocationCount) {
                                complete(invocationCount);
                                return myItems;
                            }
                        });
                        return Unit.INSTANCE$;
                    }
                }
        );
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/basic/multifile/" + getTestName(false) + "/";
    }
}
