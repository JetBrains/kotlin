// WITH_STDLIB
// FULL_JDK
// !LANGUAGE: +ProgressionsChangingResolve -DisableCheckingChangedProgressionsResolve
// This test is not K1/K2 identical due to KT-58789 not implemented yet

// FILE: JavaSmartList.java
import kotlin.ranges.ClosedRange;

import java.util.Collection;

public class JavaSmartList <E> {
    JavaSmartList(E x) {}
    JavaSmartList(Collection<E> x) {}

    static void append(Object x) {}
    static void append(Collection<?> x) {}

    static void append2(Iterable<?> x) {}
    static void append2(Collection<?> x) {}

    public static class In <T> {
        In(T x) {}
    }

    static void append3(In<?> x) {}
    static void append3(In<Collection<?>> x) {}

    static <E> void append4(E x) {}
    static <E extends Collection<?>> void append4(E x) {}

    static <T> void takes(T x) {}
    static <T extends Collection<?> & ClosedRange<?>> void takes(T x) {}
}

// FILE: main.kt
fun main() {
    JavaSmartList(1..2) // warning
    JavaSmartList<IntRange>(1..10) // no warning

    JavaSmartList.append(1..10)    // warning
    JavaSmartList.append((1..10) as Any) // no warning
    JavaSmartList.append((1..10) as Iterable<Int>) // no warning
    JavaSmartList.append("a".."z") // no warning, the range is not iterable
    JavaSmartList.append(1.0..2.0)

    JavaSmartList.append2(1..10)    // no warning

    JavaSmartList.append3(JavaSmartList.In(1..10))    // no warning

    JavaSmartList.append4(1..10)    // warning

    JavaSmartList.append4<IntRange>(1..10)    // warning

    JavaSmartList.takes(1..10)    // warning
}