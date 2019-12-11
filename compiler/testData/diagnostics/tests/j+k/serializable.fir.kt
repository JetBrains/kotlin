// FILE: B.kt

import aa.A.use
import aa.A.useList

fun testPrimitives(b: Byte, ss: Short, i: Int, l: Long, d: Double, s: String, f: Float, bool: Boolean) {
    <!INAPPLICABLE_CANDIDATE!>use<!>(b)
    <!INAPPLICABLE_CANDIDATE!>use<!>(ss)
    <!INAPPLICABLE_CANDIDATE!>use<!>(i)
    <!INAPPLICABLE_CANDIDATE!>use<!>(l)
    <!INAPPLICABLE_CANDIDATE!>use<!>(s)
    <!INAPPLICABLE_CANDIDATE!>use<!>(f)
    <!INAPPLICABLE_CANDIDATE!>use<!>(d)
    <!INAPPLICABLE_CANDIDATE!>use<!>(bool)
}

class N
class S: java.io.Serializable

fun testArrays(ia: IntArray, ai: Array<Int>, an: Array<N>, a: Array<S>) {
    <!INAPPLICABLE_CANDIDATE!>use<!>(ia)
    <!INAPPLICABLE_CANDIDATE!>use<!>(ai)
    <!INAPPLICABLE_CANDIDATE!>use<!>(an)
    <!INAPPLICABLE_CANDIDATE!>use<!>(a)
}

fun testLiterals() {
    <!INAPPLICABLE_CANDIDATE!>use<!>(1)
    <!INAPPLICABLE_CANDIDATE!>use<!>(1.0)
    <!INAPPLICABLE_CANDIDATE!>use<!>(11111111111111)
    <!INAPPLICABLE_CANDIDATE!>use<!>("Asdsd")
    <!INAPPLICABLE_CANDIDATE!>use<!>(true)
}

fun testNotSerializable(l: List<Int>) {
    <!INAPPLICABLE_CANDIDATE!>use<!>(l)
    <!INAPPLICABLE_CANDIDATE!>use<!>(N())
}

enum class C {
    E, E2
}

fun testEnums(a: Enum<*>) {
    <!INAPPLICABLE_CANDIDATE!>use<!>(C.E)
    <!INAPPLICABLE_CANDIDATE!>use<!>(C.E2)
    <!INAPPLICABLE_CANDIDATE!>use<!>(a)
}

fun testLists(a: List<Int>) {
    <!INAPPLICABLE_CANDIDATE!>useList<!>(a)
}

// FILE: aa/A.java
package aa;

public class A {
    public static void use(java.io.Serializable s) { }
    public static void useList(java.util.List<? extends java.io.Serializable> s) { }
}