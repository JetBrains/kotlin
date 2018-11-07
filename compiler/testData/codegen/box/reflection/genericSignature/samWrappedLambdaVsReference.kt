// SKIP_JDK6
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK

// FILE: Java.java

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.*;

public final class Java {
    public static Method getTest(Predicate<Object> predicate) throws NoSuchMethodException {
        return predicate.getClass().getMethod("test", Object.class);
    }

    public static Method getFoo(ComplexPredicate<Long> predicate) throws NoSuchMethodException {
        return predicate.getClass().getMethod("foo", Number.class, List.class, Map.class);
    }
}

// FILE: ComplexPredicate.java

import java.util.*;

public interface ComplexPredicate<T extends Number> {
    boolean foo(T p0, List<T> p1, Map<T, ? extends Set<T>> p2);
}

// FILE: K.kt

import java.lang.reflect.Method
import java.util.*
import kotlin.test.assertEquals

fun check(title: String, expected: String, method: Method) {
    assertEquals(expected, method.parameterTypes.contentToString(), "Fail parameterTypes of $title")
    assertEquals(expected, method.genericParameterTypes.contentToString(), "Fail genericParameterTypes of $title")
}

private fun bar(p0: Long, p1: List<Long>, p2: Map<Long, Set<Long>>) = true

fun box(): String {
    check("SAM-implementing lambda 1", "[class java.lang.Object]", Java.getTest { Objects.nonNull(it) })
    check("SAM-wrapped function reference 1", "[class java.lang.Object]", Java.getTest(Objects::nonNull))

    check("SAM-implementing lambda 2", "[class java.lang.Number, interface java.util.List, interface java.util.Map]", Java.getFoo { p0, p1, p2 -> bar(p0, p1, p2) })
    check("SAM-wrapped function reference 2", "[class java.lang.Number, interface java.util.List, interface java.util.Map]", Java.getFoo(::bar))

    return "OK"
}
