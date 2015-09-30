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

import java.util.*;

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

    public static void throwCce(String message) {
        throw sanitizeStackTrace(new ClassCastException(message));
    }

    public static void throwUninitializedPropertyAccessException(String propertyName) {
        throw sanitizeStackTrace(new UninitializedPropertyAccessException(propertyName));
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

    public static boolean isMutableIterator(Object obj) {
        return (obj instanceof Iterator) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableIterator));
    }

    public static Iterator asMutableIterator(Object obj) {
        Iterator result = null;
        try {
            result = (Iterator) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not an Iterator");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterator)) {
            throwCce("argument is not a MutableIterator");
        }
        return result;
    }

    public static Iterator safeAsMutableIterator(Object obj) {
        Iterator result;
        try {
            result = (Iterator) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterator)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableListIterator(Object obj) {
        return (obj instanceof ListIterator) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableListIterator));
    }

    public static ListIterator asMutableListIterator(Object obj) {
        ListIterator result = null;
        try {
            result = (ListIterator) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not a ListIterator");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableListIterator)) {
            throwCce("argument is not a MutableListIterator");
        }
        return result;
    }

    public static ListIterator safeAsMutableListIterator(Object obj) {
        ListIterator result;
        try {
            result = (ListIterator) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableListIterator)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableIterable(Object obj) {
        return (obj instanceof Iterable) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableIterable));
    }

    public static Iterable asMutableIterable(Object obj) {
        Iterable result = null;
        try {
            result = (Iterable) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not an Iterable");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterable)) {
            throwCce("argument is not a MutableIterable");
        }
        return result;
    }

    public static Iterable safeAsMutableIterable(Object obj) {
        Iterable result;
        try {
            result = (Iterable) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterable)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableCollection(Object obj) {
        return (obj instanceof Collection) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableCollection));
    }

    public static Collection asMutableCollection(Object obj) {
        Collection result = null;
        try {
            result = (Collection) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not a Collection");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableCollection)) {
            throwCce("argument is not a MutableCollection");
        }
        return result;
    }

    public static Collection safeAsMutableCollection(Object obj) {
        Collection result;
        try {
            result = (Collection) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableCollection)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableList(Object obj) {
        return (obj instanceof List) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableList));
    }

    public static List asMutableList(Object obj) {
        List result = null;
        try {
            result = (List) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not a List");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableList)) {
            throwCce("argument is not a MutableList");
        }
        return result;
    }

    public static List safeAsMutableList(Object obj) {
        List result;
        try {
            result = (List) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableList)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableSet(Object obj) {
        return (obj instanceof Set) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableSet));
    }

    public static Set asMutableSet(Object obj) {
        Set result = null;
        try {
            result = (Set) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not a Set");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableSet)) {
            throwCce("argument is not a MutableSet");
        }
        return result;
    }

    public static Set safeAsMutableSet(Object obj) {
        Set result;
        try {
            result = (Set) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableSet)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableMap(Object obj) {
        return (obj instanceof Map) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableMap));
    }

    public static Map asMutableMap(Object obj) {
        Map result = null;
        try {
            result = (Map) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not a Map");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap)) {
            throwCce("argument is not a MutableMap");
        }
        return result;
    }

    public static Map safeAsMutableMap(Object obj) {
        Map result = null;
        try {
            result = (Map) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap)) {
            return null;
        }
        return result;
    }

    public static boolean isMutableMapEntry(Object obj) {
        return (obj instanceof Map.Entry) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableMap.Entry));
    }

    public static Map.Entry asMutableMapEntry(Object obj) {
        Map.Entry result = null;
        try {
            result = (Map.Entry) obj;
        }
        catch (ClassCastException e) {
            throwCce("argument is not a Map.Entry");
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap.Entry)) {
            throwCce("argument is not a MutableMap.MutableEntry");
        }
        return result;
    }

    public static Map.Entry safeAsMutableMapEntry(Object obj) {
        Map.Entry result = null;
        try {
            result = (Map.Entry) obj;
        }
        catch (ClassCastException e) {
            return null;
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap.Entry)) {
            return null;
        }
        return result;
    }
}
