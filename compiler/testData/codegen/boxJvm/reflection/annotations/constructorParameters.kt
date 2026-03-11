// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    public J(@Anno("J") String s) {}

    public class Inner {
        public Inner(@Anno("Inner") int x) {}
    }
}

// FILE: test/JEnum.java
package test;

public enum JEnum {
    ;
    JEnum(@Anno("JEnum") double d) {}
}

// FILE: test/box.kt
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Anno(val value: String)

class K(@Anno("K") f: Float) {
    inner class Inner(@Anno("Inner") j: Long)
}

enum class KEnum(@Anno("KEnum") z: Boolean)

sealed class Sealed(@Anno("Sealed") s: Short)

class Default(@Anno("Default") n: Number? = null)

sealed class SealedWithDefault(@Anno("SealedWithDefault") a: Any? = null)

private val KClass<*>.ctorParamAnnotations: String
    get() = constructors.single().parameters.joinToString(", ") { p ->
        p.annotations.map { (it as Anno).value }.toString()
    }

fun box(): String {
    assertEquals("[J]", J::class.ctorParamAnnotations)
    assertEquals("[], [Inner]", J.Inner::class.ctorParamAnnotations)
    assertEquals("[JEnum]", JEnum::class.ctorParamAnnotations)

    assertEquals("[K]", K::class.ctorParamAnnotations)
    assertEquals("[], [Inner]", K.Inner::class.ctorParamAnnotations)
    assertEquals("[KEnum]", KEnum::class.ctorParamAnnotations)
    assertEquals("[Sealed]", Sealed::class.ctorParamAnnotations)
    assertEquals("[Default]", Default::class.ctorParamAnnotations)
    assertEquals("[SealedWithDefault]", SealedWithDefault::class.ctorParamAnnotations)

    return "OK"
}
