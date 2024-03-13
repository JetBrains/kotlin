/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework;


import com.intellij.util.ReflectionUtil;

import java.util.Hashtable;
import java.util.Map;

public final class Cleanup {
    private Cleanup() { }

    public static void cleanupSwingDataStructures() throws Throwable {
        Object manager = ReflectionUtil.getDeclaredMethod(Class.forName("javax.swing.KeyboardManager"), "getCurrentManager").invoke(null);
        Map<?, ?> componentKeyStrokeMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "componentKeyStrokeMap");
        componentKeyStrokeMap.clear();
        Map<?, ?> containerMap = ReflectionUtil.getField(manager.getClass(), manager, Hashtable.class, "containerMap");
        containerMap.clear();
    }
}
