// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

enum class KEnum { A }

fun test(e: KEnum): String {
    return e.declaringClass.toString()
}

fun box(): String {
    val result = test(KEnum.A)
    return if (result == "class KEnum") "OK" else "fail: $result"
}