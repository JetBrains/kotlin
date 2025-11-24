// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect open class LibA() {
    open fun fromLib(): String
}

// MODULE: lib-inter()()(lib-common)
fun LibA.fromInter(): String = "libInter${fromLib()}"

// MODULE: lib-platform()()(lib-inter)
actual open class LibA actual constructor() {
    actual open fun fromLib(): String = "libPlatform"
}

// MODULE: app-common(lib-common)
open class AppA : LibA() {
    override fun fromLib(): String = "app"
    fun fromApp(): String = "appOnly"
}

fun useCommon(a: LibA): String = a.fromLib()

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun useInter(a: LibA): String = a.fromInter()

// MODULE: app-platform(lib-platform)()(app-inter)
fun usePlatform(a: LibA): String {
    val fromCommon = useCommon(a)
    val fromInter = useInter(a)
    val isApp = a is AppA
    val appPart = if (a is AppA) a.fromApp() else "noApp"
    return "$fromCommon$fromInter$isApp$appPart"
}

fun box(): String {
    val a = AppA()
    val r = usePlatform(a)
    return if (r == "applibInterapptrueappOnly") "OK" else "FAIL"
}
