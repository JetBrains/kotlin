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

import kotlin.jvm.internal.markers.*;

import java.util.*;

@SuppressWarnings("unused")
public class TypeIntrinsics {
    public static void throwCce(Object argument, String requestedClassName) {
        String argumentClassName = argument == null ? "null" : argument.getClass().getName();
        ClassCastException classCastException = new ClassCastException(argumentClassName + " cannot be cast to " + requestedClassName);
        throw Intrinsics.sanitizeStackTrace(classCastException, TypeIntrinsics.class.getName());
    }

    public static void throwCce(ClassCastException e) {
        throw Intrinsics.sanitizeStackTrace(e, TypeIntrinsics.class.getName());
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterator)) {
            throwCce(obj, "kotlin.MutableIterator");
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableListIterator)) {
            throwCce(obj, "kotlin.MutableListIterator");
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterable)) {
            throwCce(obj, "kotlin.MutableIterable");
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableCollection)) {
            throwCce(obj, "kotlin.MutableCollection");
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableList)) {
            throwCce(obj, "kotlin.MutableList");
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableSet)) {
            throwCce(obj, "kotlin.MutableSet");
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap)) {
            throwCce(obj, "kotlin.MutableMap");
        }
        return result;
    }

    public static Map safeAsMutableMap(Object obj) {
        Map result;
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
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap.Entry)) {
            throwCce(obj, "kotlin.MutableMap.MutableEntry");
        }
        return result;
    }

    public static Map.Entry safeAsMutableMapEntry(Object obj) {
        Map.Entry result;
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
