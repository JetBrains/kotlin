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

package org.jetbrains.jet.editor;

import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;

/**
 * @author Evgeny Gerashchenko
 * @since 7/16/12
 */
public class TypedHandlerTest extends LightCodeInsightTestCase {
    public void testTypeStringTemplateStart() throws Exception {
        configureFromFileText("a.kt", "val x = \"$<caret>\"");
        EditorTestUtil.performTypingAction(getEditor(), '{');
        checkResultByText("val x = \"${}\"");
    }
}
