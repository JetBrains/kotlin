interface I<T1, T2>

abstract class C1<T> : I<T, String>
abstract class C2 : I<Int, String>
abstract class C3 : I<String, Int>
abstract class C4<T> : I<String, T>

fun <T> foo(i: I<T, String>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object: I<...>{...}" }
// EXIST: { itemText: "object: C1<...>(){...}" }
// EXIST: { itemText: "object: C2(){...}" }
// EXIST: { itemText: "object: C4<String>(){...}" }
// EXIST: { itemText: "enumValueOf" }
// NOTHING_ELSE
