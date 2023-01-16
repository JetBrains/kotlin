// WITH_REFLECT
// TARGET_BACKEND: JVM

import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.kotlinProperty
import kotlin.test.assertEquals

class A {
    val x = "outer"  // NB: backing field of this property has the name `x$1`, to avoid conflict with public field moved from companion.
    val y = "outer"  // Same here, `y$1`.

    companion object {
        @JvmField
        val x = "companion"

        const val y = "companion"
    }
}

fun test(f: KProperty<*>) {
    val javaField = f.javaField
        ?: error("javaField == null for $f")

    assertEquals(f, javaField.kotlinProperty, "Incorrect kotlinProperty for $javaField")
}

fun box(): String {
    test(A::x)
    test(A::y)

    // We have to use reflection API to get companion object properties if we want to test the invariant `p.javaField.kotlinProperty == p`.
    // If we used the callable reference syntax instead `A.Companion::x`, we'd get a bound property reference, which is an instance of
    // `KProperty0`. Whereas `kotlinProperty` always return unbound reference, so a `KProperty1`.
    test(A.Companion::class.declaredMemberProperties.single { it.name == "x" })
    test(A.Companion::class.declaredMemberProperties.single { it.name == "y" })

    return "OK"
}
