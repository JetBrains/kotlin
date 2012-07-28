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

package org.jetbrains.jet.compiler.android;

import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * @author Natalia.Ukhorskaya
 */

public class AbstractCodegenTestCaseOnAndroid extends TestCase {

    protected void invokeBoxMethod(String filePath) throws Exception {
        try {
            Class clazz;
            clazz = Class.forName(filePath.replaceAll("\\\\|-|\\.|/", "_") + ".namespace");
            Method method;
            method = clazz.getMethod("box");
            assertEquals("OK", method.invoke(null));
        }
        catch (Throwable e) {
            throw new RuntimeException("File: " + filePath, e);
        }
    }
}
