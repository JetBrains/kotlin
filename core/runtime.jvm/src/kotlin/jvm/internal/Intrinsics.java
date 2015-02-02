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

package kotlin.jvm.internal;

import kotlin.IntRange;
import kotlin.KotlinNullPointerException;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class Intrinsics {
    private Intrinsics() {
    }

    public static String stringPlus(String self, Object other) {
        return String.valueOf(self) + String.valueOf(other);
    }

    public static void throwNpe() {
        throw sanitizeStackTrace(new KotlinNullPointerException());
    }

    public static void checkExpressionValueIsNotNull(Object value, String message) {
        if (value == null) {
            IllegalStateException exception = new IllegalStateException(message + " must not be null");
            throw sanitizeStackTrace(exception);
        }
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
            throwParameterIsNullException(paramName);
        }
    }

    private static void throwParameterIsNullException(String paramName) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        // #0 Thread.getStackTrace()
        // #1 Intrinsics.throwParameterIsNullException
        // #2 Intrinsics.checkParameterIsNotNull
        // #3 our caller
        StackTraceElement caller = stackTraceElements[3];
        String className = caller.getClassName();
        String methodName = caller.getMethodName();

        IllegalArgumentException exception =
                new IllegalArgumentException("Parameter specified as non-null is null: " +
                                             "method " + className + "." + methodName +
                                             ", parameter " + paramName);
        throw sanitizeStackTrace(exception);
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

    // This method is not used from generated code anymore but kept for backwards compatibility
    @Deprecated
    public static IntRange arrayIndices(int length) {
        return new IntRange(0, length - 1);
    }

    private static void throwUndefinedForReified() {
        throw new UnsupportedOperationException("You should not use functions with reified parameter without inline");
    }

    public static void reifyNewArray(String typeParameterIdentifier) {
        throwUndefinedForReified();
    }

    public static void reifyCheckcast(String typeParameterIdentifier) {
        throwUndefinedForReified();
    }

    public static void reifyInstanceof(String typeParameterIdentifier) {
        throwUndefinedForReified();
    }

    public static void reifyJavaClass(String typeParameterIdentifier) {
        throwUndefinedForReified();
    }

    public static void needClassReification() {
        throwUndefinedForReified();
    }

    private static <T extends Throwable> T sanitizeStackTrace(T throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int size = stackTrace.length;

        int lastIntrinsic = -1;
        for (int i = 0; i < size; i++) {
            if (Intrinsics.class.getName().equals(stackTrace[i].getClassName())) {
                lastIntrinsic = i;
            }
        }

        List<StackTraceElement> list = Arrays.asList(stackTrace).subList(lastIntrinsic + 1, size);
        throwable.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return throwable;
    }
}
