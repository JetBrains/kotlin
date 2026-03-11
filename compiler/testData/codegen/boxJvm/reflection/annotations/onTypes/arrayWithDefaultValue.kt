// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.test.assertEquals

@Target(AnnotationTarget.TYPE)
annotation class A(val values: Array<String> = ["Fail"])

fun f(): @A Unit {}

fun box(): String {
    assertEquals("@test.A()", ::f.returnType.annotations.single().toString())
    return "OK"
}
