// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

import java.util.*;

public class J {
    public static List<? extends Number> listExtends() { return null; }
    public static List<? super Number> listSuper() { return null; }
    public static List<?> listStar() { return null; }
    public static Collection<? extends Number> collectionExtends() { return null; }
    public static Collection<?> collectionStar() { return null; }
    public static Iterable<? extends Number> iterableExtends() { return null; }
    public static Iterable<? super Number> iterableSuper() { return null; }
    public static Iterable<?> iterableStar() { return null; }
    public static Iterator<? extends Number> iteratorExtends() { return null; }
    public static Iterator<?> iteratorStar() { return null; }
    public static Map<String, ? extends Number> mapExtends() { return null; }
    public static Map<String, ? super Number> mapSuper() { return null; }
    public static Map<?, ?> mapStar() { return null; }
    public static Map.Entry<? extends String, ? extends Number> entryExtends() { return null; }
    public static Map.Entry<?, ?> entryStar() { return null; }
    public static Comparable<? super Number> comparableSuper() { return null; }
    public static Comparable<? extends Number> comparableExtends() { return null; }
    public static Comparable<?> comparableStar() { return null; }
    public static Enum<? extends Enum<?>> enumExtends() { return null; }
    public static Enum<?> enumStar() { return null; }
    public static KOut<? extends Number> kOutExtends() { return null; }
    public static KOut<? super Number> kOutSuper() { return null; }
    public static KOut<?> kOutStar() { return null; }
    public static KIn<? super Number> kInSuper() { return null; }
    public static KIn<? extends Number> kInExtends() { return null; }
    public static KIn<?> kInStar() { return null; }
    public static KInv<? extends Number> kInvExtends() { return null; }
    public static KInv<? super Number> kInvSuper() { return null; }
    public static KInv<?> kInvStar() { return null; }
    public static JInv<? extends Number> jInvExtends() { return null; }
    public static JInv<? super Number> jInvSuper() { return null; }
    public static JInv<?> jInvStar() { return null; }
    public static List<List<? extends Number>> nested() { return null; }
    public static List<List<?>> nestedStar() { return null; }
}

// FILE: test/JInv.java
package test;

public class JInv<T> {}

// FILE: K.kt
package test

import kotlin.reflect.*
import kotlin.test.assertEquals

class KOut<out T>
class KIn<in T>
class KInv<T>

fun check(callable: KCallable<*>, expected: String) {
    assertEquals(expected, callable.returnType.toString())
}

fun box(): String {
    check(J::listExtends, "(kotlin.collections.MutableList<out kotlin.Number!>..kotlin.collections.List<kotlin.Number!>?)")
    check(J::listSuper, "kotlin.collections.MutableList<in kotlin.Number!>!")
    check(J::listStar, "kotlin.collections.(Mutable)List<*>!")
    check(J::collectionExtends, "(kotlin.collections.MutableCollection<out kotlin.Number!>..kotlin.collections.Collection<kotlin.Number!>?)")
    check(J::collectionStar, "kotlin.collections.(Mutable)Collection<*>!")
    check(J::iterableExtends, "kotlin.collections.(Mutable)Iterable<kotlin.Number!>!")
    check(J::iterableSuper, "kotlin.collections.(Mutable)Iterable<*>!")
    check(J::iterableStar, "kotlin.collections.(Mutable)Iterable<*>!")
    check(J::iteratorExtends, "kotlin.collections.(Mutable)Iterator<kotlin.Number!>!")
    check(J::iteratorStar, "kotlin.collections.(Mutable)Iterator<*>!")
    check(J::mapExtends, "(kotlin.collections.MutableMap<kotlin.String!, out kotlin.Number!>..kotlin.collections.Map<kotlin.String!, kotlin.Number!>?)")
    check(J::mapSuper, "kotlin.collections.MutableMap<kotlin.String!, in kotlin.Number!>!")
    check(J::mapStar, "kotlin.collections.(Mutable)Map<*, *>!")
    check(J::entryExtends, "(kotlin.collections.MutableMap.MutableEntry<out kotlin.String!, out kotlin.Number!>..kotlin.collections.Map.Entry<kotlin.String!, kotlin.Number!>?)")
    check(J::entryStar, "kotlin.collections.(Mutable)Map.(Mutable)Entry<*, *>!")
    check(J::comparableSuper, "kotlin.Comparable<kotlin.Number!>!")
    check(J::comparableExtends, "kotlin.Comparable<*>!")
    check(J::comparableStar, "kotlin.Comparable<*>!")
    check(J::enumExtends, "kotlin.Enum<out kotlin.Enum<*>!>!")
    check(J::enumStar, "kotlin.Enum<*>!")
    check(J::kOutExtends, "test.KOut<kotlin.Number!>!")
    check(J::kOutSuper, "test.KOut<*>!")
    check(J::kOutStar, "test.KOut<*>!")
    check(J::kInSuper, "test.KIn<kotlin.Number!>!")
    check(J::kInExtends, "test.KIn<*>!")
    check(J::kInStar, "test.KIn<*>!")
    check(J::kInvExtends, "test.KInv<out kotlin.Number!>!")
    check(J::kInvSuper, "test.KInv<in kotlin.Number!>!")
    check(J::kInvStar, "test.KInv<*>!")
    check(J::jInvExtends, "test.JInv<out kotlin.Number!>!")
    check(J::jInvSuper, "test.JInv<in kotlin.Number!>!")
    check(J::jInvStar, "test.JInv<*>!")
    check(J::nested, "kotlin.collections.(Mutable)List<(kotlin.collections.MutableList<out kotlin.Number!>..kotlin.collections.List<kotlin.Number!>?)>!")
    check(J::nestedStar, "kotlin.collections.(Mutable)List<kotlin.collections.(Mutable)List<*>!>!")

    return "OK"
}
