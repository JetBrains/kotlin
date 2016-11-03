// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun box(): String {
    val f = "KOTLIN"::get
    return "${f(1)}${f(0)}"
}
