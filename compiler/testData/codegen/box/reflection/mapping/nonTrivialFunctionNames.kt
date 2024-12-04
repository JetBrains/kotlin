// WITH_REFLECT
// TARGET_BACKEND: JVM

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

class A {
    @JvmName("jvmFoo")
    fun foo(s: String): Int = s.length

    fun mangled(z: Z): Number = z.value

    internal fun internal(s: String): Int = s.length


    // Some different members with similar JVM signatures, to check that kotlinFunction doesn't incorrectly resolve into one of these.
    private fun jvmFoo(s: String): String = s
    private fun `mangled-IQRRRT4`(z: Number) {}
    private fun `internal$main`(s: String): String = s
}

@JvmInline
value class Z(val value: Number)

fun test(f: KFunction<*>) {
    val javaMethod = f.javaMethod
        ?: error("javaMethod == null for $f")
    assertEquals(f, javaMethod.kotlinFunction, "Incorrect kotlinFunction for $javaMethod")
}

fun box(): String {
    test(A::foo)
    test(A::mangled)
    test(A::internal)

    return "OK"
}
