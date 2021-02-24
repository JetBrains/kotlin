// TARGET_BACKEND: JVM

fun box(): String {
    val o = "O"
    var result = ""

    val r = Runnable { result = o + "K" } //capturing local vals and local var
    r.run()
    return result
}