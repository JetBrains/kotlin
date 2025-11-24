// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect open class LibA()

fun <T> useGeneric(t: T): String where T : LibA = "libGeneric"

// MODULE: lib-inter()()(lib-common)
fun <T> useGenericInter(t: T): String where T : LibA = "libGenericInter"

// MODULE: lib-platform()()(lib-inter)
actual open class LibA actual constructor()

// MODULE: app-common(lib-common)
class AppA : LibA()

fun appCommonUse(a: AppA): String = useGeneric(a)

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun appInterUse(a: AppA): String = appCommonUse(a) + useGenericInter(a)

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String {
    val r = appInterUse(AppA())
    return if (r == "libGenericlibGenericInter") "OK" else "FAIL"
}
