// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: -PropertyParamAnnotationDefaultTargetMode
package test

import kotlin.reflect.KProperty1
import kotlin.test.assertEquals

annotation class Anno(val value: Int)

annotation class A(val x: String, @Anno(1) val y: String)

fun box(): String {
    // No annotation here because before 2.4, the annotation was applied only to the parameter.
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
