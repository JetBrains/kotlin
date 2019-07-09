interface I<T1, T2>

abstract class C1<T> : I<T, String>
abstract class C2 : I<Int, String>
abstract class C3 : I<String, Int>
abstract class C4<T> : I<String, T>

fun <T> foo(i: I<T, String>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object : I<...>{...}" }
// EXIST: { itemText: "object : C1<...>(){...}" }
// EXIST: { itemText: "object : C2(){...}" }
// EXIST: { itemText: "object : C4<String>(){...}" }

// all these items shouldn't be proposed, see KT-15479
// EXIST: { itemText: "enumValueOf" }
// EXIST: { itemText: "maxOf", tailText: "(a: T, b: T) (kotlin.comparisons)" }
// EXIST: { itemText: "maxOf", tailText: "(a: T, b: T, c: T) (kotlin.comparisons)" }
// EXIST: { itemText: "minOf", tailText: "(a: T, b: T) (kotlin.comparisons)" }
// EXIST: { itemText: "minOf", tailText: "(a: T, b: T, c: T) (kotlin.comparisons)" }
// NOTHING_ELSE
