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

import org.apache.commons.lang.StringUtils;
import org.jetbrains.jet.ConfigurationKind;

public class SuperGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    public void testBasicProperty () {
        blackBoxFile("/super/basicproperty.kt");
    }

    public void testTraitProperty () {
        blackBoxFile("/super/traitproperty.kt");
    }

    public void testBasicMethodSuperTrait () {
        blackBoxFile("/super/basicmethodSuperTrait.kt");
    }

    public void testBasicMethodSuperClass () {
        blackBoxFile("/super/basicmethodSuperClass.kt");
    }

    public void testInnerClassLabeledSuper() {
        blackBoxFile("super/innerClassLabeledSuper.kt");
    }

    public void testInnerClassLabeledSuperProperty() {
        blackBoxFile("super/innerClassLabeledSuperProperty.kt");
    }

    public void testMultipleSuperTraits() {
        blackBoxFile("super/multipleSuperTraits.kt");
    }

    public void testEnclosedFun () {
        blackBoxFile("/super/enclosedFun.kt");
    }

    public void testEnclosedVar () {
        blackBoxFile("/super/enclosedVar.kt");
    }

    public void testKt2887() {
        loadFile("super/kt2887.kt");
        String text = generateToText();
        // There should be exactly one bridge in this example
        assertEquals(1, StringUtils.countMatches(text, "bridge"));
    }
}
