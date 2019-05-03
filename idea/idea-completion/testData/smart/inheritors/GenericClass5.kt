interface I<T1>

abstract class C<T1, T2> : I<T1>

fun foo(i: I<String>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object : I<String>{...}" }
// EXIST: { itemText: "object : C<...>(){...}" }
// NOTHING_ELSE
