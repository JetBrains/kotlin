// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect class LibA {
    val t: String
}

fun LibA.libExt(): String = "libExt${this.t}"

// MODULE: lib-inter()()(lib-common)
fun LibA.libInterExt(): String = "libInterExt${this.t}"

// MODULE: lib-platform()()(lib-inter)
actual class LibA constructor(
    actual val t: String
)

fun LibA.libPlatformExt(): String = "libPlatformExt${this.t}"

// MODULE: app-common(lib-common)
typealias AppA = LibA

fun AppA.appExt(): String = "appExt${this.t}"

fun appCommonUse(a: AppA): String = a.libExt() + a.appExt()

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun appInterUse(a: AppA): String {
    val fromCommon = appCommonUse(a)
    val fromLibInter = a.libInterExt()
    return "$fromCommon$fromLibInter"
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun appPlatformUse(a: AppA): String {
    val fromInter = appInterUse(a)
    val fromPlatform = a.libPlatformExt()
    return "$fromInter$fromPlatform"
}

fun box(): String {
    val a = LibA("OK")
    val r = appPlatformUse(a)
    return if (r == "libExtOKappExtOKlibInterExtOKlibPlatformExtOK") "OK" else "FAIL"
}
