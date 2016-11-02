interface X
interface Y : X
interface Z

interface I<T>

abstract class C1<T> : I<T>
abstract class C2 : I<Y>
abstract class C3 : I<Z>

fun <T : X> foo(i: I<T>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object: I<...>{...}" }
// EXIST: { itemText: "object: C1<X>(){...}" }
// EXIST: { itemText: "object: C2(){...}" }
// EXIST: { itemText: "enumValueOf" }
// NOTHING_ELSE
