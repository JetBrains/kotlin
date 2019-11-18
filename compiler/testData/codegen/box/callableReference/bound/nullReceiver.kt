// IGNORE_BACKEND_FIR: JVM_IR
val String?.ok: String
    get() = "OK"

fun box() = (null::ok).get()