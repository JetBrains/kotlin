class One {
    val a1 = arrayOf(
            object { val fy = "text"}
    )}

fun box() = if (One().a1[0].fy == "text") "OK" else "fail"