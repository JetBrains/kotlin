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

import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class EnumGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testSuperclassIsEnum() throws Exception {
        loadFile("enum/simple.kt");
        Class<?> season = generateClass("Season");
        assertEquals("java.lang.Enum", season.getSuperclass().getName());
    }

    public void testEnumClassModifiers() throws Exception {
        loadFile("enum/simple.kt");
        Class<?> season = generateClass("Season");
        int modifiers = season.getModifiers();
        assertTrue((modifiers & 0x4000) != 0); // ACC_ENUM
        assertTrue((modifiers & Modifier.FINAL) != 0);
    }

    public void testEnumFieldModifiers() throws Exception {
        loadFile("enum/simple.kt");
        Class<?> season = generateClass("Season");
        Field summer = season.getField("SUMMER");
        int modifiers = summer.getModifiers();
        assertTrue((modifiers & 0x4000) != 0); // ACC_ENUM
        assertTrue((modifiers & Modifier.FINAL) != 0);
        assertTrue((modifiers & Modifier.STATIC) != 0);
        assertTrue((modifiers & Modifier.PUBLIC) != 0);
    }

    public void testEnumConstantConstructors() throws Exception {
        loadText("enum class Color(val rgb: Int) { RED(0xFF0000), GREEN(0x00FF00); }");
        Class<?> colorClass = generateClass("Color");
        Field redField = colorClass.getField("RED");
        Object redValue = redField.get(null);
        Method rgbMethod = colorClass.getMethod("getRgb");
        assertEquals(0xFF0000, rgbMethod.invoke(redValue));
    }

    public void testNoClassForSimpleEnum() throws Exception {
        loadFile("enum/name.kt");
        Class<?> cls = generateClass("State");
        Field field = cls.getField("O");
        assertEquals("State", field.get(null).getClass().getName());
    }

    public void testYesClassForComplexEnum() throws Exception {
        loadFile("enum/openMethod.kt");
        Class<?> cls = generateClass("IssueState");
        Field field = cls.getField("DEFAULT");
        assertEquals("IssueState", field.get(null).getClass().getName());
        field = cls.getField("FIXED");
        assertEquals("IssueState", field.getType().getName());
        assertEquals("IssueState$FIXED", field.get(null).getClass().getName());
        assertNotNull(cls.getClassLoader().loadClass("IssueState$FIXED"));
        try {
            cls.getClassLoader().loadClass("IssueState$DEFAULT");
            fail();
        }
        catch (ClassNotFoundException e) {
            // ok
        }
    }
}
