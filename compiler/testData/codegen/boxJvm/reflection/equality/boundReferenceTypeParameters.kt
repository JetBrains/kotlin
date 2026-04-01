// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J {
    public <T> T j(T t) { return t; }

    public class Inner<U> {
        public Inner(U u) {}
    }
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class C {
    fun <X> f(x: X): X = x

    inner class Inner<Z>(val z: Z)
}

fun <Y> Any.g(y: Y): Y = y

private fun checkEqual(x: Any, y: Any) {
    assertEquals(x, y)
    assertEquals(y, x)
    assertEquals(x.hashCode(), y.hashCode())
}

private fun checkEqualTypeParameters(bound: Any, unbound: Any) {
    checkEqual((bound as KCallable<*>).typeParameters, (unbound as KCallable<*>).typeParameters)

    // Even if we drop the unbound instance/extension parameter, value parameters are not equal because they have different indices.
    if (unbound.parameters.isNotEmpty()) {
        // Constructor of `J.Inner` is loaded incorrectly in K1-based reflection, see KT-85313.
        if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) != true) {
            assertNotEquals(bound.parameters, unbound.parameters.drop(1))
        }
    }
}

fun box(): String {
    val fileClass = object {}::class.java.enclosingClass

    val f1: (String) -> String = C()::f
    val f2 = C::class.members.single { it.name == "f" }
    checkEqualTypeParameters(f1, f2)

    val g1: (String) -> String = Any()::g
    val g2 = fileClass.declaredMethods.single { it.name == "g" }.kotlinFunction!!
    checkEqualTypeParameters(g1, g2)

    val i1: (String) -> C.Inner<String> = C()::Inner
    val i2 = C.Inner::class.constructors.single()
    checkEqualTypeParameters(i1, i2)

    val j1: (String) -> String = J()::j
    val j2 = J::class.members.single { it.name == "j" }
    checkEqualTypeParameters(j1, j2)

    val ji1: (String) -> J.Inner<String> = J()::Inner
    val ji2 = J.Inner::class.constructors.single()
    checkEqualTypeParameters(ji1, ji2)

    return "OK"
}
