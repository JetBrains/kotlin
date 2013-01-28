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

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.ConfigurationKind;

/**
 * Test correct code is generated for descriptors loaded with alt jdk annotations
 */
public class JdkAnnotationsTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    public void testArrayList() {
        blackBoxFile("jdk-annotations/arrayList.kt");
    }

    public void testHashMap() {
        blackBoxFile("jdk-annotations/hashMap.kt");
    }

    //moved from PrimitiveTypesTest
    public void testKt1397() {
        blackBoxFile("regressions/kt1397.kt");
    }

    public void testIteratingOverHashMap() {
        blackBoxFile("jdk-annotations/iteratingOverHashMap.kt");
    }
}
