fun overloadFun(p: Int) {}
fun overloadFun(p: String) {}

fun <T> ambiguityFun(vararg fn: (T) -> Unit) {}

fun overloadContext() {
    ambiguityFun({<caret> x: String -> overloadFun(x) }, ::overloadFun)
}