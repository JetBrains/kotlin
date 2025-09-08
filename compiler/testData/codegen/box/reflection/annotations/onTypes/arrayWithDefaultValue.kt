// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

package test

import kotlin.test.assertEquals

@Target(AnnotationTarget.TYPE)
annotation class A(val values: Array<String> = ["Fail"])

fun f(): @A Unit {}

fun box(): String {
    assertEquals("@test.A()", ::f.returnType.annotations.single().toString())
    return "OK"
}
