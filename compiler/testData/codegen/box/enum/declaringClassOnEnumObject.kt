// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: UNRESOLVED_REFERENCE (declaringClass). See https://youtrack.jetbrains.com/issue/KT-49653. To be discussed.
package test

enum class KEnum { A }

fun test(e: KEnum): String {
    return e.declaringClass.toString()
}

fun box(): String {
    val result = test(KEnum.A)
    return if (result == "class test.KEnum") "OK" else "fail: $result"
}