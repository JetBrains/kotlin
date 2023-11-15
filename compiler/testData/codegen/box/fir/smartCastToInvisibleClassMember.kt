// ISSUE: KT-63164
// DUMP_IR
// FIR_IDENTICAL

// MODULE: m1
// FILE: info.kt

internal class Info {
    val status: String = "OK"
}

val info: Any? = Info()

// MODULE: m2(m1)
// FILE: box.kt

fun getStatus(param: Any?): String {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    if (param is Info) {
        return param.status
    }
    return "NO STATUS"
}

fun box(): String = getStatus(info)
