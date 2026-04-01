// IGNORE_BACKEND_K1: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode
package test

import kotlin.reflect.KProperty1
import kotlin.test.assertEquals

annotation class Anno(val value: Int)

annotation class A(val x: String, @Anno(1) val y: String)

fun box(): String {
    // We still use K1 for member properties, and K1 lacks the support of annotations in metadata.
    // As soon as we switch to Km-based properties, annotation will appear here.
    assertEquals(
        "[[], []]",
        A::class.members.filter { it is KProperty1<*, *> }.map { it.annotations }.toString(),
    )

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // K1 did not support annotations on annotation parameters.
        assertEquals(
            "[[], []]",
            A::class.constructors.single().parameters.map { it.annotations }.toString(),
        )
    } else {
        assertEquals(
            "[[], [@test.Anno(value=1)]]",
            A::class.constructors.single().parameters.map { it.annotations }.toString(),
        )
    }
    return "OK"
}
