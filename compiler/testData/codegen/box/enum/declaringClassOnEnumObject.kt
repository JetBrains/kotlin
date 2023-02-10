// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// K2 status: declaringClass is error for enums since Kotlin 1.9
// LANGUAGE: -ProhibitEnumDeclaringClass

package test

enum class KEnum { A }

fun test(e: KEnum): String {
    return e.declaringClass.toString()
}

fun box(): String {
    val result = test(KEnum.A)
    return if (result == "class test.KEnum") "OK" else "fail: $result"
}
