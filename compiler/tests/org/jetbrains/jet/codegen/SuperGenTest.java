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

public class SuperGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.JDK_HEADERS);
    }

    public void testBasicProperty () {
        blackBoxFile("/super/basicproperty.jet");
//        System.out.println(generateToText());
    }

    public void testTraitProperty () {
        blackBoxFile("/super/traitproperty.jet");
//        System.out.println(generateToText());
    }

    public void testBasicMethodSuperTrait () {
        blackBoxFile("/super/basicmethodSuperTrait.jet");
//        System.out.println(generateToText());
    }

    public void testBasicMethodSuperClass () {
        blackBoxFile("/super/basicmethodSuperClass.jet");
//        System.out.println(generateToText());
    }

    public void testEnclosedFun () {
        blackBoxFile("/super/enclosedFun.jet");
//        System.out.println(generateToText());
    }

    public void testEnclosedVar () {
        blackBoxFile("/super/enclosedVar.jet");
//        System.out.println(generateToText());
    }

}
