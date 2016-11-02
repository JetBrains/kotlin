interface I<T>

abstract class C1<T> : I<T>
abstract class C2 : I<String>
abstract class C3 : I<Int>

fun <T> foo(i: I<T>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object: I<...>{...}" }
// EXIST: { itemText: "object: C1<...>(){...}" }
// EXIST: { itemText: "object: C2(){...}" }
// EXIST: { itemText: "object: C3(){...}" }
// EXIST: { itemText: "enumValueOf" }
// NOTHING_ELSE
