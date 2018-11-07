// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals

interface A1 {
    fun a1()
}

interface A2 {
    fun a2()
}

interface B1 : A1
interface B2 : A1, A2

interface C : B2

abstract class D : B1, C

fun box(): String {
    assertEquals("public abstract void A1.a1()", D::a1.javaMethod!!.toString())
    assertEquals("public abstract void A2.a2()", D::a2.javaMethod!!.toString())

    return "OK"
}
