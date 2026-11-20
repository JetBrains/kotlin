// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J1.java
public @interface J1 {
    String[] value();
    String[] other();
}

// FILE: J2.java
public @interface J2 {
    long[] value();
    long[] other();
}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun check(klass: KClass<*>, parameterName: String, isVararg: Boolean, expectedToString: String) {
    val param = klass.constructors.single().parameters.single { it.name == parameterName }
    assertEquals(isVararg, param.isVararg)
    assertEquals(expectedToString, param.type.toString())
}

fun box(): String {
    check(J1::class, "value", isVararg = true, "kotlin.Array<out kotlin.String>")
    check(J1::class, "other", isVararg = false, "kotlin.Array<kotlin.String>")

    // Only the constructor parameter is vararg, not the property.
    assertEquals("kotlin.Array<kotlin.String>", J1::class.members.single { it.name == "value" }.returnType.toString())

    check(J2::class, "value", isVararg = true, "kotlin.LongArray")
    check(J2::class, "other", isVararg = false, "kotlin.LongArray")

    return "OK"
}
