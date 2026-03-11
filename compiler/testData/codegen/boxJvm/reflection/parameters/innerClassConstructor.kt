// TARGET_BACKEND: JVM
// WITH_REFLECT
// JAVAC_OPTIONS: -parameters
// FILE: J.java
public class J {
    public J(String s1) {}
    public class Inner {
        public Inner(String s2, String s3) {}
    }
}

// FILE: box.kt
import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class K(val s1: String) {
    inner class Inner(val s2: String, val s3: String = "K")
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
    val unboundK = K::Inner
    assertEquals(3, unboundK.parameters.size)
    unboundK.check(0, null, KParameter.Kind.INSTANCE, K::class, false)
    unboundK.check(1, "s2", KParameter.Kind.VALUE, String::class, false)
    unboundK.check(2, "s3", KParameter.Kind.VALUE, String::class, true)

    val boundK = K("O")::Inner
    assertEquals(2, boundK.parameters.size)
    boundK.check(0, "s2", KParameter.Kind.VALUE, String::class, false)
    boundK.check(1, "s3", KParameter.Kind.VALUE, String::class, true)

    val unboundJ = J::Inner
    assertEquals(3, unboundJ.parameters.size)
    unboundJ.check(0, null, KParameter.Kind.INSTANCE, J::class, false)
    unboundJ.check(1, "s2", KParameter.Kind.VALUE, String::class, false)
    unboundJ.check(2, "s3", KParameter.Kind.VALUE, String::class, false)

    val boundJ = J("O")::Inner
    assertEquals(2, boundJ.parameters.size)
    boundJ.check(0, "s2", KParameter.Kind.VALUE, String::class, false)
    boundJ.check(1, "s3", KParameter.Kind.VALUE, String::class, false)

    return "OK"
}
