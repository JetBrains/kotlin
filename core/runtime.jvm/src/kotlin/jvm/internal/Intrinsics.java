/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package kotlin.jvm.internal;

import kotlin.IntRange;
import kotlin.KotlinNullPointerException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class Intrinsics {
    private Intrinsics() {
    }

    public static String stringPlus(String self, Object other) {
        return String.valueOf(self) + String.valueOf(other);
    }

    public static void throwNpe() {
        throw new KotlinNullPointerException();
    }

    public static void checkReturnedValueIsNotNull(Object value, String className, String methodName) {
        if (value == null) {
            IllegalStateException exception =
                    new IllegalStateException("Method specified as non-null returned null: " + className + "." + methodName);
            throw sanitizeStackTrace(exception);
        }
    }

    public static void checkFieldIsNotNull(Object value, String className, String fieldName) {
        if (value == null) {
            IllegalStateException exception =
                    new IllegalStateException("Field specified as non-null is null: " + className + "." + fieldName);
            throw sanitizeStackTrace(exception);
        }
    }

    public static void checkParameterIsNotNull(Object value, String paramName) {
        if (value == null) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

            // #0 is Thread.getStackTrace(), #1 is Intrinsics.checkParameterIsNotNull, #2 is our caller
            StackTraceElement caller = stackTraceElements[2];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();

            IllegalArgumentException exception =
                    new IllegalArgumentException("Parameter specified as non-null is null: " +
                                                 "method " + className + "." + methodName +
                                                 ", parameter " + paramName);
            throw sanitizeStackTrace(exception);
        }
    }

    public static int compare(long thisVal, long anotherVal) {
        return thisVal < anotherVal ? -1 : thisVal == anotherVal ? 0 : 1;
    }

    public static int compare(int thisVal, int anotherVal) {
        return thisVal < anotherVal ? -1 : thisVal == anotherVal ? 0 : 1;
    }

    public static boolean areEqual(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    public static IntRange arrayIndices(int length) {
        return new IntRange(0, length - 1);
    }

    private static final Set<String> METHOD_NAMES_TO_SKIP = new HashSet<String>(Arrays.asList(
            "throwNpe", "checkReturnedValueIsNotNull", "checkFieldIsNotNull", "checkParameterIsNotNull"
    ));

    public static <T extends Throwable> T sanitizeStackTrace(T throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        ArrayList<StackTraceElement> list = new ArrayList<StackTraceElement>(stackTrace.length);
        boolean skip = true;
        for (StackTraceElement element : stackTrace) {
            if (!skip) {
                list.add(element);
            }
            else if ("kotlin.jvm.internal.Intrinsics".equals(element.getClassName())) {
                if (METHOD_NAMES_TO_SKIP.contains(element.getMethodName())) {
                    skip = false;
                }
            }
        }
        throwable.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return throwable;
    }
}
