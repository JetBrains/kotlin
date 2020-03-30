// KJS_WITH_FULL_RUNTIME
//WITH_RUNTIME
fun box(): String {
    val a = intArrayOf(1, 2)
    val b = arrayOf("OK")
    if ((a::component2)() != 2) {
        return "fail"
    }

    if ((a::get)(1) != 2) {
        return "fail"
    }

    return (b::get)(0)
}