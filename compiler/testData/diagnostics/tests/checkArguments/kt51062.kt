// WITH_STDLIB
// FULL_JDK

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
    JavaSmartList(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("constructor JavaSmartList<E : Any!>(x: (Mutable)Collection<E!>!)")!>1..2<!>) // warning
    JavaSmartList<IntRange>(1..10) // no warning

    JavaSmartList.append(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun append(x: (Mutable)Collection<*>!): Unit")!>1..10<!>)    // warning
    JavaSmartList.append((1..10) as Any) // no warning
    JavaSmartList.append((1..10) as Iterable<Int>) // no warning
    JavaSmartList.append("a".."z") // no warning, the range is not iterable
    JavaSmartList.append(1.0..2.0)

    JavaSmartList.append2(1..10)    // no warning

    JavaSmartList.append3(JavaSmartList.In(1..10))    // no warning

    JavaSmartList.append4(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun <E : (Mutable)Collection<*>!> append4(x: E!): Unit")!>1..10<!>)    // warning

    JavaSmartList.append4<IntRange>(1..10)    // warning

    JavaSmartList.takes(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun <T : (Mutable)Collection<*>!> takes(x: T!): Unit where T : ClosedRange<*>!")!>1..10<!>)    // warning
}
