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

import kotlin.KotlinNullPointerException;
import kotlin.UninitializedPropertyAccessException;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class Intrinsics {
    private Intrinsics() {
    }

    public static String stringPlus(String self, Object other) {
        return self + other;
    }

    public static void checkNotNull(Object object) {
        if (object == null) {
            throwNpe();
        }
    }

    public static void checkNotNull(Object object, String message) {
        if (object == null) {
            throwNpe(message);
        }
    }

    public static void throwNpe() {
        throw sanitizeStackTrace(new KotlinNullPointerException());
    }

    public static void throwNpe(String message) {
        throw sanitizeStackTrace(new KotlinNullPointerException(message));
    }

    public static void throwUninitializedProperty(String message) {
        throw sanitizeStackTrace(new UninitializedPropertyAccessException(message));
    }

    public static void throwUninitializedPropertyAccessException(String propertyName) {
        throwUninitializedProperty("lateinit property " + propertyName + " has not been initialized");
    }

    public static void throwAssert() {
        throw sanitizeStackTrace(new AssertionError());
    }

    public static void throwAssert(String message) {
        throw sanitizeStackTrace(new AssertionError(message));
    }

    public static void throwIllegalArgument() {
        throw sanitizeStackTrace(new IllegalArgumentException());
    }

    public static void throwIllegalArgument(String message) {
        throw sanitizeStackTrace(new IllegalArgumentException(message));
    }

    public static void throwIllegalState() {
        throw sanitizeStackTrace(new IllegalStateException());
    }

    public static void throwIllegalState(String message) {
        throw sanitizeStackTrace(new IllegalStateException(message));
    }

    public static void checkExpressionValueIsNotNull(Object value, String expression) {
        if (value == null) {
            throw sanitizeStackTrace(new IllegalStateException(expression + " must not be null"));
        }
    }

    public static void checkNotNullExpressionValue(Object value, String message) {
        if (value == null) {
            throw sanitizeStackTrace(new IllegalStateException(message));
        }
    }

    public static void checkReturnedValueIsNotNull(Object value, String className, String methodName) {
        if (value == null) {
            throw sanitizeStackTrace(
                    new IllegalStateException("Method specified as non-null returned null: " + className + "." + methodName)
            );
        }
    }

    public static void checkReturnedValueIsNotNull(Object value, String message) {
        if (value == null) {
            throw sanitizeStackTrace(new IllegalStateException(message));
        }
    }

    public static void checkFieldIsNotNull(Object value, String className, String fieldName) {
        if (value == null) {
            throw sanitizeStackTrace(new IllegalStateException("Field specified as non-null is null: " + className + "." + fieldName));
        }
    }

    public static void checkFieldIsNotNull(Object value, String message) {
        if (value == null) {
            throw sanitizeStackTrace(new IllegalStateException(message));
        }
    }

    public static void checkParameterIsNotNull(Object value, String paramName) {
        if (value == null) {
            throwParameterIsNullException(paramName);
        }
    }

    public static void checkNotNullParameter(Object value, String message) {
        if (value == null) {
            throw sanitizeStackTrace(new IllegalArgumentException(message));
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

    public static boolean areEqual(Double first, Double second) {
        return first == null ? second == null : second != null && first.doubleValue() == second.doubleValue();
    }

    public static boolean areEqual(Double first, double second) {
        return first != null && first.doubleValue() == second;
    }

    public static boolean areEqual(double first, Double second) {
        return second != null && first == second.doubleValue();
    }

    public static boolean areEqual(Float first, Float second) {
        return first == null ? second == null : second != null && first.floatValue() == second.floatValue();
    }

    public static boolean areEqual(Float first, float second) {
        return first != null && first.floatValue() == second;
    }

    public static boolean areEqual(float first, Float second) {
        return second != null && first == second.floatValue();
    }

    public static void throwUndefinedForReified() {
        throwUndefinedForReified(
                "This function has a reified type parameter and thus can only be inlined at compilation time, not called directly."
        );
    }

    public static void throwUndefinedForReified(String message) {
        throw new UnsupportedOperationException(message);
    }

    public static void reifiedOperationMarker(int id, String typeParameterIdentifier) {
        throwUndefinedForReified();
    }

    public static void reifiedOperationMarker(int id, String typeParameterIdentifier, String message) {
        throwUndefinedForReified(message);
    }

    public static void needClassReification() {
        throwUndefinedForReified();
    }

    public static void needClassReification(String message) {
        throwUndefinedForReified(message);
    }

    public static void checkHasClass(String internalName) throws ClassNotFoundException {
        String fqName = internalName.replace('/', '.');
        try {
            Class.forName(fqName);
        }
        catch (ClassNotFoundException e) {
            throw sanitizeStackTrace(new ClassNotFoundException(
                    "Class " + fqName + " is not found. Please update the Kotlin runtime to the latest version", e
            ));
        }
    }

    public static void checkHasClass(String internalName, String requiredVersion) throws ClassNotFoundException {
        String fqName = internalName.replace('/', '.');
        try {
            Class.forName(fqName);
        }
        catch (ClassNotFoundException e) {
            throw sanitizeStackTrace(new ClassNotFoundException(
                    "Class " + fqName + " is not found: this code requires the Kotlin runtime of version at least " + requiredVersion, e
            ));
        }
    }

    private static <T extends Throwable> T sanitizeStackTrace(T throwable) {
        return sanitizeStackTrace(throwable, Intrinsics.class.getName());
    }

    static <T extends Throwable> T sanitizeStackTrace(T throwable, String classNameToDrop) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int size = stackTrace.length;

        int lastIntrinsic = -1;
        for (int i = 0; i < size; i++) {
            if (classNameToDrop.equals(stackTrace[i].getClassName())) {
                lastIntrinsic = i;
            }
        }

        List<StackTraceElement> list = Arrays.asList(stackTrace).subList(lastIntrinsic + 1, size);
        throwable.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return throwable;
    }
}
