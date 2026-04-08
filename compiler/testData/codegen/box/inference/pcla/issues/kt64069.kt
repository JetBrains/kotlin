// ISSUE: KT-64069
// WITH_STDLIB

private var enable: Boolean = true
private val string: String? by lazy {
    if (enable) {
        getT()
    } else {
        null
    }
}

fun <T> getT(): T {
    return "OK" as T
}

fun box() = string!!
