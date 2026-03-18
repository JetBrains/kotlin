// TARGET_BACKEND: JVM
// WITH_REFLECT

import java.io.*
import kotlin.test.assertEquals

class A {
    fun foo(s: String = "", vararg xs: Long): String = "foo"
}

fun check(x: Any) {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(x)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    assertEquals(x, ois.readObject())
    ois.close()
}

fun coercionToUnit(f: (A, String, LongArray) -> Unit): Any = f
fun varargToElement(f: (A, String, Long, Long) -> String): Any = f
fun defaultAndVararg(f: (A) -> String): Any = f
fun allOfTheAbove(f: (A) -> Unit): Any = f

fun box(): String {
    check(coercionToUnit(A::foo))
    check(varargToElement(A::foo))
    check(defaultAndVararg(A::foo))
    check(allOfTheAbove(A::foo))

    return "OK"
}
