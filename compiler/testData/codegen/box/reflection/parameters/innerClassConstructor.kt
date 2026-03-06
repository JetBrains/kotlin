// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class Outer(val s1: String) {
    inner class Inner(val s2: String, val s3: String = "K") {
        val result = s1 + s2 + s3
    }
}

fun KFunction<*>.check(index: Int, name: String?, kind: KParameter.Kind, klass: KClass<*>, isOptional: Boolean) {
    val p = parameters[index]
    assertEquals(index, p.index)
    assertEquals(name, p.name)
    assertEquals(kind, p.kind)
    assertEquals(klass, p.type.classifier)
    assertEquals(isOptional, p.isOptional)
    assertFalse(p.isVararg)
}

fun box(): String {
    val unbound = Outer::Inner
    assertEquals(3, unbound.parameters.size)
    unbound.check(0, null, KParameter.Kind.INSTANCE, Outer::class, false)
    unbound.check(1, "s2", KParameter.Kind.VALUE, String::class, false)
    unbound.check(2, "s3", KParameter.Kind.VALUE, String::class, true)

    val bound = Outer("O")::Inner
    assertEquals(2, bound.parameters.size)
    bound.check(0, "s2", KParameter.Kind.VALUE, String::class, false)
    bound.check(1, "s3", KParameter.Kind.VALUE, String::class, true)

    return "OK"
}
