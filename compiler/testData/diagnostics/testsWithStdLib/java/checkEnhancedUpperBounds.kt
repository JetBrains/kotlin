// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// FULL_JDK

// FILE: MapLike.java
import java.util.Map;

public class MapLike<@org.jetbrains.annotations.NotNull K, V> {
    void putAll(Map<K, V> map);
}

// FILE: ListLike.java
import java.util.Collection;

public class ListLike<K extends Collection<@org.jetbrains.annotations.NotNull Object>> {}

// FILE: main.kt
fun test0(map : MapLike<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>Int?<!>, Int>) {}
fun <K> test11(map : MapLike<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>K<!>, K>) {}
fun <K> test12(map : MapLike<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>K?<!>, K>) {}
fun <K : Any> test13(map : MapLike<K, K>) {}
fun <K : Any> test14(map : MapLike<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>K?<!>, K>) {}

class Foo<K>

typealias A<A> = MapLike<A, Int>
typealias A2<B> = Foo<MapLike<B, Int>>
typealias A3<C> = ListLike<List<C>>

fun main1(x: A<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>Int?<!>>) {}
fun main2(x: A2<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>Int?<!>>) {}
fun main3(x: <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>A3<Int?><!>) {}
fun main3() {
    val x = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>A3<Int?>()<!> // TODO: support reporting errors on typealias constructor calls
    val x2 = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>A<Int?>()<!> // TODO: support reporting errors on typealias constructor calls
    val y: <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>A3<Int?><!> = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS!>A3<Int?>()<!>
}
