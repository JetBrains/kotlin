// TARGET_BACKEND: JVM
package test

enum class KEnum { A }

fun test(e: KEnum): String {
    return e.declaringClass.toString()
}

fun box(): String {
    val result = test(KEnum.A)
    return if (result == "class test.KEnum") "OK" else "fail: $result"
}