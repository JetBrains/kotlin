// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode
package test

import kotlin.reflect.KProperty1
import kotlin.test.assertEquals

annotation class Anno(val value: Int)

annotation class A(val x: String, @Anno(1) val y: String)

fun box(): String {
    val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

    // K1 did not support annotations on annotation parameters.
    assertEquals(
        if (useK1) "[[], []]" else "[[], [@test.Anno(value=1)]]",
        A::class.members.filter { it is KProperty1<*, *> }.map { it.annotations }.toString(),
    )
    assertEquals(
        if (useK1) "[[], []]" else "[[], [@test.Anno(value=1)]]",
        A::class.constructors.single().parameters.map { it.annotations }.toString(),
    )
    return "OK"
}
