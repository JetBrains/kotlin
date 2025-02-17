/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework;


import com.intellij.util.ReflectionUtil;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

public final class Cleanup {
    private Cleanup() {
    }

    private static Method getCurrentManagerMethod;

    static {
        try {
            Class<?> keyboardManagerClass = Class.forName("javax.swing.KeyboardManager");
            getCurrentManagerMethod = keyboardManagerClass.getDeclaredMethod("getCurrentManager");
            getCurrentManagerMethod.setAccessible(true);
        }
        catch (Throwable e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    public static void cleanupSwingDataStructures() throws Throwable {
        Object manager = getCurrentManagerMethod.invoke(null);
        Map<?, ?> componentKeyStrokeMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "componentKeyStrokeMap");
        componentKeyStrokeMap.clear();
        Map<?, ?> containerMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "containerMap");
        containerMap.clear();
    }
}
