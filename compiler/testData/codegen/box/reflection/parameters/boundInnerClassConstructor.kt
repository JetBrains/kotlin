// TARGET_BACKEND: JVM

// WITH_REFLECT
import kotlin.reflect.KParameter
import kotlin.test.assertEquals

class Outer(val s1: String) {
    inner class Inner(val s2: String, val s3: String = "K") {
        val result = s1 + s2 + s3
    }
}

fun KParameter.check(name: String) {
    assertEquals(name, this.name!!)
    assertEquals(KParameter.Kind.VALUE, this.kind)
}

fun box(): String {
    val ctor = Outer("O")::Inner
    val ctorPararms = ctor.parameters

    ctorPararms[0].check("s2")
    ctorPararms[1].check("s3")

    return "OK"
}