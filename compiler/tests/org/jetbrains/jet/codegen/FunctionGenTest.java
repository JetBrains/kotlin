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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class FunctionGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testAnyEqualsNullable() throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.equals(\"lala\")");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testNoRefToOuter() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        loadText("class A() { fun f() : ()->String { val s = \"OK\"; return { -> s } } }");
        Class foo = generateClass("A");
        Object obj = foo.newInstance();
        Method f = foo.getMethod("f");
        Object closure = f.invoke(obj);
        Class<? extends Object> aClass = closure.getClass();
        Field[] fields = aClass.getDeclaredFields();
        assertEquals(1, fields.length);
        assertEquals("$s", fields[0].getName());
    }

    public void testAnyEquals() throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.equals(\"lala\")");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testRecursion() throws Exception {
        loadFile("box/functions/recursion.kt");
        ClassFileFactory classFileFactory = generateClassesInFile();

        final BindingTrace bindingTrace = classFileFactory.getState().getBindingTrace();
        List<JetCallExpression > tailRecursions =
                new ArrayList<JetCallExpression>(
                Collections2.filter(bindingTrace.getKeys(BindingContext.TAIL_RECURSION_CALL), new Predicate<JetCallExpression>() {
                    @Override
                    public boolean apply(JetCallExpression input) {
                        //noinspection ConstantConditions
                        return bindingTrace.get(BindingContext.TAIL_RECURSION_CALL, input).isDoGenerateTailRecursion();
                    }
                })
            );

        Collections.sort(tailRecursions, new Comparator<JetCallExpression>() {
            @Override
            public int compare(@NotNull JetCallExpression o1, @NotNull JetCallExpression o2) {
                return o1.getTextOffset() - o2.getTextOffset();
            }
        });

        List<String> texts = new ArrayList<String>(tailRecursions.size());
        for (JetCallExpression recursion : tailRecursions) {
            texts.add(recursion.getText());
        }

        //System.out.println(Joiner.on(",\n").skipNulls().join(Lists.transform(texts, new Function<String, String>() {
        //    @Override
        //    public String apply(@Nullable String input) {
        //        return input == null ? "" : ("\"" + input.replace("\"", "\\\"") + "\"");
        //    }
        //}))); // useful for debugging and fixing list

        assertEquals(
                Arrays.asList(
                        "a(counter + 1, text, e + 1, \"tail 7\")",
                        "a2(counter - 1, \"tail 17\")",
                        "b(acounter + 1, \"tail 25\")",
                        "b(acounter + 1, \"tail 29\")",
                        "b(acounter + 1, \"tail 40\")",
                        "c(counter + 1, \"tail 44\")",
                        "d(counter + 1, \"tail 48\")",
                        "g3(counter - 1, \"tail 97\")",
                        "f1(\"tail 104\")",
                        "f2(\"tail 108\")",
                        "h(counter - 1, \"tail 119\")",
                        "repeat(num - 1, acc.append(this)!!)",
                        "escape(i + 1, result + escapeChar(get(i)))",
                        "foldl(foldFunction(next(), acc), foldFunction)",
                        "withWhen(counter - 1, \"tail\")",
                        "badTails(x - 1, \"tail\")"
                ),
                texts
        );
    }
}
