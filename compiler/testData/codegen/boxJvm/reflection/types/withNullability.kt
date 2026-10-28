// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public interface J {
    String strF();
}

// FILE: K.kt

import kotlin.reflect.full.withNullability
import kotlin.test.assertEquals

fun str(): String = ""
fun strN(): String? = ""

class C<T> {
    fun t(): T = null!!
    fun tN(): T? = null
    fun tDNN(): T & Any = null!!
}

fun box(): String {
    val str = ::str.returnType
    val strN = ::strN.returnType
    val strF = J::strF.returnType

    assertEquals(str, strN.withNullability(false))
    assertEquals(strN, strN.withNullability(true))
    assertEquals(str, str.withNullability(false))
    assertEquals(strN, str.withNullability(true))

    assertEquals(str, strF.withNullability(false))
    assertEquals(strN, strF.withNullability(true))

    val t = C<*>::t.returnType
    val tN = C<*>::tN.returnType
    val tDNN = C<*>::tDNN.returnType

    assertEquals(t, tN.withNullability(false))
    assertEquals(tN, tN.withNullability(true))
    assertEquals(t, t.withNullability(false))
    assertEquals(tN, t.withNullability(true))

    assertEquals(tDNN, tDNN.withNullability(false))
    assertEquals(tN, tDNN.withNullability(true))

    return "OK"
}
