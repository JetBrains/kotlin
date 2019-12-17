// IGNORE_BACKEND_FIR: JVM_IR

fun test(ix: Int?): String {
    val arr = arrayOf("fail 1")

    if (ix != null) {
        arr[ix] = "OK"
        return arr[ix]
    }
    return "fail 2"
}

fun box() = test(0)

