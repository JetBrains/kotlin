// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
private class One {
    val a1 = arrayOf(
            object { val fy = "text"}
    )
}

fun box() = if (One().a1[0].fy == "text") "OK" else "fail"