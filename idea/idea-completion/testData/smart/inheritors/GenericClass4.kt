interface I<T1>

abstract class C<T1, T2> : I<T1>

fun <T> foo(i: I<T>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object : I<...>{...}" }
// EXIST: { itemText: "object : C<...>(){...}" }

// all these items shouldn't be proposed, see KT-15479
// EXIST: { itemText: "enumValueOf" }
// EXIST: { itemText: "maxOf", tailText: "(a: T, b: T) (kotlin.comparisons)" }
// EXIST: { itemText: "maxOf", tailText: "(a: T, b: T, c: T) (kotlin.comparisons)" }
// EXIST: { itemText: "minOf", tailText: "(a: T, b: T) (kotlin.comparisons)" }
// EXIST: { itemText: "minOf", tailText: "(a: T, b: T, c: T) (kotlin.comparisons)" }
// NOTHING_ELSE
