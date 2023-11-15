// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE, WASM
// FIR status: see KT-63164
// ISSUE: KT-63164
// DUMP_IR

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
