// Enable when callable references to builtin members is supported
// IGNORE_BACKEND: JS, NATIVE
fun box(): String {
    val f = "KOTLIN"::get
    return "${f(1)}${f(0)}"
}
