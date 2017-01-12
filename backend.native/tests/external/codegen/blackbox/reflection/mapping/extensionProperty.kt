// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class K(var value: Long)

var K.ext: Double
    get() = value.toDouble()
    set(value) {
        this.value = value.toLong()
    }

fun box(): String {
    val p = K::ext

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(getter, Class.forName("ExtensionPropertyKt").getMethod("getExt", K::class.java))
    assertEquals(setter, Class.forName("ExtensionPropertyKt").getMethod("setExt", K::class.java, Double::class.java))

    val k = K(42L)
    assert(getter.invoke(null, k) == 42.0) { "Fail k getter" }
    setter.invoke(null, k, -239.0)
    assert(getter.invoke(null, k) == -239.0) { "Fail k setter" }

    return "OK"
}
