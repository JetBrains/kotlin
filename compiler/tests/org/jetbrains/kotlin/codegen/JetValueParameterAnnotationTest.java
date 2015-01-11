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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.annotation.Annotation;

@SuppressWarnings("deprecation")
public class JetValueParameterAnnotationTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    private static class ValueParameter {
        public final String name;
        public final String type;

        private ValueParameter(@NotNull String name, @NotNull String type) {
            this.name = name;
            this.type = type;
        }
    }

    @NotNull
    private static ValueParameter valueParameter(@NotNull String name, boolean nullable) {
        return new ValueParameter(name, nullable ? "?" : "");
    }

    public void testOneNotNullParam() {
        doTest(
                "fun foo(byte: Byte) {}",
                valueParameter("byte", false)
        );
    }

    public void testTwoNotNullParams() {
        doTest(
                "fun foo(int: Int, string: String) {}",
                valueParameter("int", false),
                valueParameter("string", false)
        );
    }

    public void testTwoNullableParams() {
        doTest(
                "fun foo(long: Long?, unit: Unit?) {}",
                valueParameter("long", true),
                valueParameter("unit", true)
        );
    }

    public void testTwoMixedParams() {
        doTest(
                "fun foo(short: Short?, boolean: Boolean) {}",
                valueParameter("short", true),
                valueParameter("boolean", false)
        );
    }

    public void testNotNullReceiver() {
        doTest(
                "fun Int.foo() {}",
                valueParameter("$receiver", false)
        );
    }

    public void testNullableReceiver() {
        doTest(
                "fun String?.foo() {}",
                valueParameter("$receiver", true)
        );
    }

    private void doTest(@NotNull String code, @NotNull ValueParameter... expected) {
        loadText(code);

        Annotation[][] annotations = generateFunction().getParameterAnnotations();
        assertSize(expected.length, annotations);

        for (int i = 0, length = annotations.length; i < length; i++) {
            assertSize(1, annotations[i]);
            Annotation annotation = annotations[i][0];
            assertEquals("jet.runtime.typeinfo.JetValueParameter", annotation.annotationType().getName());
            assertEquals(expected[i].name, CodegenTestUtil.getAnnotationAttribute(annotation, "name"));
            assertEquals(expected[i].type, CodegenTestUtil.getAnnotationAttribute(annotation, "type"));
        }
    }
}
