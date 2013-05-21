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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.collect.Lists;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JvmClassNameTest {
    @Test
    public void signatureName() {
        testSignatureName("jet/Map", "jet/Map", "jet.Map", "jet.Map", Collections.<String>emptyList());
    }

    @Test
    public void signatureNameOfInnerClass() {
        testSignatureName("jet/Map.Entry", "jet/Map$Entry", "jet.Map.Entry", "jet.Map", Lists.newArrayList("Entry"));
    }

    @Test
    public void signatureNameOfDeepInnerClass() {
        testSignatureName("jet/Map.Entry.AAA", "jet/Map$Entry$AAA", "jet.Map.Entry.AAA", "jet.Map", Lists.newArrayList("Entry", "AAA"));
    }

    @Test
    public void simpleName() {
        testSignatureName("jet", "jet", "jet", "jet", Collections.<String>emptyList());
    }

    @Test
    public void incorrectSignature() {
        testSignatureName("jet/Map.", "jet/Map$", "jet.Map.", "jet.Map", Lists.newArrayList(""));

    }

    private static void testSignatureName(
            String className,
            String innerClassName,
            String fqName,
            String outerClassName,
            List<String> innerClassNameList
    ) {
        JvmClassName mapEntryName = JvmClassName.bySignatureName(className);
        assertEquals(innerClassName, mapEntryName.getInternalName());
        assertEquals(fqName, mapEntryName.getFqName().asString());
        assertEquals(outerClassName, mapEntryName.getOuterClassFqName().asString());
        assertEquals(innerClassNameList, mapEntryName.getInnerClassNameList());
    }
}
