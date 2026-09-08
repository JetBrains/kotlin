// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.TypeVariable
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

interface A {
    fun <R> f(x: R): R
    val <T> List<T>.p: T get() = this[0]
}
interface B : A
interface C : A
interface D : B, C

fun box(): String {
    val f = D::class.members.single { it.name == "f" }.returnType.javaType
    assertEquals("R", (f as TypeVariable<*>).name)
    assertEquals(A::class.java.getDeclaredMethod("f", Any::class.java), f.genericDeclaration)

    val p = D::class.members.single { it.name == "p" }.returnType.javaType
    assertEquals("T", (p as TypeVariable<*>).name)

    return "OK"
}
