interface I<T>

abstract class C1<T> : I<T>
abstract class C2 : I<String>
abstract class C3 : I<Int>
class C4<T>(t: T) : I<T>
class C5<T1, T2>(t1: T1, t2: T2) : I<T2>

fun foo(i: I<Int>){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "object : I<Int>{...}" }
// EXIST: { itemText: "object : C1<Int>(){...}" }
// EXIST: { itemText: "object : C3(){...}" }
// EXIST: { itemText: "C4", tailText: "(t: Int) (<root>)" }
// EXIST: { itemText: "C5", tailText: "(t1: T1, t2: Int) (<root>)" }
// NOTHING_ELSE
