// TARGET_BACKEND: JVM
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME

lateinit var bar: String

fun box(): String {
    if (::bar.isInitialized) return "Fail 1"
    bar = "OK"
    if (!::bar.isInitialized) return "Fail 2"
    return bar
}
