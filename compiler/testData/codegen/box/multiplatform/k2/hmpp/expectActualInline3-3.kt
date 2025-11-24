// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect class LibBox {
    val v: String
}

inline fun <T> LibBox.map(block: (String) -> T): T = block(v)

// MODULE: lib-inter()()(lib-common)
inline fun LibBox.interMap(): String = this.map { "inter$it" }

// MODULE: lib-platform()()(lib-inter)
actual class LibBox constructor(
    actual val v: String
)

// MODULE: app-common(lib-common)
typealias AppBox = LibBox

inline fun AppBox.appMap(block: (String) -> String): String =
    this.map(block) + "app"

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun appInterUse(b: AppBox): String {
    val fromInter = b.interMap()
    val fromApp = b.appMap { it }
    return "$fromInter$fromApp"
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String {
    val b = LibBox("OK")
    val res = appInterUse(b)
    return if (res == "interOKOKapp") "OK" else "FAIL"
}
