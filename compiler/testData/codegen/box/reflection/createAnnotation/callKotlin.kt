// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertFails

annotation class NoParams
annotation class OneDefault(val s: String = "Fail")
annotation class TwoNonDefaults(val string: String, val klass: KClass<*>)

inline fun <reified T : Annotation> create(vararg args: Any?): T =
        T::class.constructors.single().call(*args)

fun box(): String {
    create<NoParams>()
    assertFails { create<NoParams>("Fail") }

    assertFails { create<OneDefault>() }
    assertFails { create<OneDefault>(42) }
    val o = create<OneDefault>("OK")
    assertEquals("OK", o.s)

    assertFails("call() should fail because arguments were passed in an incorrect order") {
        create<TwoNonDefaults>(Any::class, "Fail")
    }
    assertFails("call() should fail because KClass (not Class) instances should be passed as arguments") {
        create<TwoNonDefaults>("Fail", Any::class.java)
    }

    val k = create<TwoNonDefaults>("OK", Int::class)
    assertEquals(Int::class, k.klass)

    return k.string
}
