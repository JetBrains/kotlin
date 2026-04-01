// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect class LibBox {
    val v: String
}

inline fun <T> LibBox.map(block: (String) -> T): T = block(v)

// MODULE: lib-platform()()(lib-common)
actual class LibBox constructor(
    actual val v: String
)

// MODULE: app-common(lib-common)
typealias AppBox = LibBox

inline fun AppBox.appMap(block: (String) -> String): String =
    this.map(block) + "app"

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    val b = LibBox("OK")
    val r = b.appMap { it }
    return if (r == "OKapp") "OK" else "FAIL"
}
