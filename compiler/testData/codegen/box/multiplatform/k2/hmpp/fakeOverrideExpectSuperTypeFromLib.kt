// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-80131
// MODULE: lib-common
expect open class Foo() {
    val size: Int
}

// MODULE: lib-inter()()(lib-common)
actual open class Foo {
    actual val size: Int = 1
}

// MODULE: lib-platform()()(lib-inter)

// MODULE: app-common(lib-common)
class Bar : Foo()

fun x(b: Bar) {
    b.size
}

// MODULE: app-inter(lib-inter)()(app-common)

// MODULE: app-platform(lib-platform)()(app-inter)
fun box() = "OK"