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

package org.jetbrains.jet.plugin.lightClasses;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.asJava.KotlinLightClass;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.plugin.libraries.NavigateToStdlibSourceRegressionTest;

public class LightClassForLibrarySourceFileTest extends NavigateToStdlibSourceRegressionTest {

    public void testLightClassForFileFromLibrarySource() throws Exception {
        PsiElement navigationElement = getNavigationElement("libraries/stdlib/src/kotlin/Iterators.kt", "FunctionIterator");
        assertTrue("FunctionIterator should navigate to JetClassOrObject", navigationElement instanceof JetClassOrObject);
        PsiClass lightClass = LightClassUtil.getPsiClass((JetClassOrObject) navigationElement);
        assertTrue("Do not create Kotlin Light Class for file from library sources", !(lightClass instanceof KotlinLightClass));
    }

}
