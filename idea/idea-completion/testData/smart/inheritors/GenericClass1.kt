interface I<T>

abstract class C1<T> : I<T>
abstract class C2 : I<String>
abstract class C3 : I<Int>

fun foo(i: I<Int>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object: I<Int>{...}" }
// EXIST: { itemText: "object: C1<Int>(){...}" }
// EXIST: { itemText: "object: C3(){...}" }
// NOTHING_ELSE
