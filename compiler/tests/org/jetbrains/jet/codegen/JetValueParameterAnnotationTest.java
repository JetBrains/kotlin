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

import jet.runtime.typeinfo.JetValueParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;

import java.lang.annotation.Annotation;

@SuppressWarnings("deprecation")
public class JetValueParameterAnnotationTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    private static JetValueParameter valueParameter(@NotNull final String name, final boolean nullable) {
        return new JetValueParameter() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String type() {
                return nullable ? "?" : "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return JetValueParameter.class;
            }
        };
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

    private void doTest(@NotNull String code, @NotNull JetValueParameter... expected) {
        loadText(code);

        Annotation[][] annotations = generateFunction().getParameterAnnotations();
        assertSize(expected.length, annotations);

        for (int i = 0, length = annotations.length; i < length; i++) {
            assertSize(1, annotations[i]);
            Annotation annotation = annotations[i][0];
            assertEquals(JetValueParameter.class, annotation.annotationType());
            assertEquals(expected[i].name(), ((JetValueParameter) annotation).name());
            assertEquals(expected[i].type(), ((JetValueParameter) annotation).type());
        }
    }
}
