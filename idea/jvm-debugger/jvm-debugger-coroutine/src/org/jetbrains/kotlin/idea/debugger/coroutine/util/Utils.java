/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Utils {
    public static Object callPrivate(
            Object instance,
            String methodName,
            Object... args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        Class[] classes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if(args[i] instanceof ReflectionSpecificType)
                classes[i] = ((ReflectionSpecificType) args[i]).specificClass();
            else
                classes[i] = args[i].getClass();
        }
        Method method = instance.getClass().getDeclaredMethod(methodName, classes);
        method.setAccessible(true);
        Object r = method.invoke(instance, args);
        return r;
    }

    public interface ReflectionSpecificType {
        Class specificClass();
    }
}
