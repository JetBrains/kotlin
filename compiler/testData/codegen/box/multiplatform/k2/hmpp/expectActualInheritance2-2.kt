// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect open class LibA() {
    open fun fromLib(): String
}

// MODULE: lib-platform()()(lib-common)
actual open class LibA actual constructor() {
    actual open fun fromLib(): String = "libPlatform"
}

// MODULE: app-common(lib-common)
open class AppA : LibA() {
    override fun fromLib(): String = "app"
    fun fromApp(): String = "appOnly"
}

fun useCommon(a: LibA): String = a.fromLib()

// MODULE: app-platform(lib-platform)()(app-common)
fun usePlatform(a: LibA): String {
    val fromCommon = useCommon(a)
    val isApp = a is AppA
    val appPart = if (a is AppA) a.fromApp() else "noApp"
    return "$fromCommon$isApp$appPart"
}

fun box(): String {
    val a = AppA()
    val r = usePlatform(a)
    return if (r == "apptrueappOnly") "OK" else "FAIL"
}
