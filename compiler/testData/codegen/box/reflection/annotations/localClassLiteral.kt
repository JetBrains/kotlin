// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Anno(val k1: KClass<*>, val k2: KClass<*>, val k3: KClass<*>)

fun box(): String {
    class L

    @Anno(k1 = L::class, k2 = Array<L?>::class, k3 = Array<out Array<L>>::class)
    class M

    val fqName = "test.LocalClassLiteralKt\$box\$L"
    assertEquals(
        "[@test.Anno(k1=class $fqName, k2=class [L$fqName;, k3=class [[L$fqName;)]",
        M::class.annotations.toString()
    )

    return "OK"
}
