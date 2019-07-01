// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.KClass

annotation class Anno(val k1: KClass<*>, val k2: KClass<*>, val k3: KClass<*>)

fun box(): String {
    class L

    @Anno(k1 = L::class, k2 = Array<L?>::class, k3 = Array<out Array<L>>::class)
    class M

    val fqName = "test.LocalClassLiteralKt\$box\$L"

    // JDK 8 and earlier
    val expected1 = "[@test.Anno(k1=class $fqName, k2=class [L$fqName;, k3=class [[L$fqName;)]"
    // JDK 9 and later
    val expected2 = "[@test.Anno(k1=$fqName.class, k2=$fqName[].class, k3=$fqName[][].class)]"

    val actual = M::class.annotations.toString()
    if (actual != expected1 && actual != expected2) return "Fail: $actual"

    return "OK"
}
