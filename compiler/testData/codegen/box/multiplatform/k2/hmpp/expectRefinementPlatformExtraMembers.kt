// LANGUAGE: +MultiPlatformProjects +ExpectRefinement
// WITH_STDLIB

// MODULE: lib-common
expect class PlatformIso {
    fun foo(): String
}

// MODULE: lib-inter()()(lib-common)
@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class PlatformIso {
    fun foo(): String
}

// MODULE: lib-platform()()(lib-inter)
actual class PlatformIso {
    actual fun foo(): String = "foo"
    fun baz(): String = "baz"
}

// MODULE: app-common(lib-common)
fun commonUse(p: PlatformIso) = p.foo()

// MODULE: app-inter(lib-inter)()(app-common)
fun interUse(p: PlatformIso) = p.foo()

// MODULE: app-platform(lib-platform)()(app-inter)
fun platformUse(p: PlatformIso) = p.foo() + p.baz()

fun box(): String {
    val p = PlatformIso()
    if (commonUse(p) != "foo") return "FAIL"
    if (interUse(p) != "foo") return "FAIL"
    if (platformUse(p) != "foobaz") return "FAIL"
    return "OK"
}
