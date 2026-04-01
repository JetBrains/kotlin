// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect open class LibA()

fun <T> useGeneric(t: T): String where T : LibA = "libGeneric"

// MODULE: lib-platform()()(lib-common)
actual open class LibA actual constructor()

// MODULE: app-common(lib-common)
class AppA : LibA()

fun appCommonUse(a: AppA): String = useGeneric(a)

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    val r = appCommonUse(AppA())
    return if (r == "libGeneric") "OK" else "FAIL"
}
