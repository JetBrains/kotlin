// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// JAVAC_SKIP
// FILE: p/J.java

package p;

public class J {
    public static J j() { return null; }
}

// FILE: k.kt

import p.*

interface Out<out T1>

fun <T> f(a: Out<T>, b: Out<T>, c: Out<T>): T = null!!
fun <T> out(t: T): Out<MutableList<T>> = null!!

fun test(a: Out<Nothing>, b: Out<MutableList<J>>) {
    val v = f(a, b, out(J.j()))
    v checkType { _<MutableList<J>>() }
    v checkType { _<MutableList<J?>>() }
}