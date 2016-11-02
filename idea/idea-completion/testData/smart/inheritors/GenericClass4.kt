interface I<T1>

abstract class C<T1, T2> : I<T1>

fun <T> foo(i: I<T>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object: I<...>{...}" }
// EXIST: { itemText: "object: C<...>(){...}" }
// EXIST: { itemText: "enumValueOf" }
// NOTHING_ELSE
