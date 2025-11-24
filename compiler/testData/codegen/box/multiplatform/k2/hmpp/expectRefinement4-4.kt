// LANGUAGE: +MultiPlatformProjects +ExpectRefinement
// WITH_STDLIB

// MODULE: lib-common
expect class A {
    fun base(): String
}

// MODULE: lib-inter1()()(lib-common)
@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class A {
    fun base(): String
    fun bar(): String
}

// MODULE: lib-inter2()()(lib-inter1)
@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class A {
    fun base(): String
    fun bar(): String
    fun baz(): String
}

// MODULE: lib-platform()()(lib-inter2)
actual class A {
    actual fun base(): String = "base"
    actual fun bar(): String = "bar"
    actual fun baz(): String = "baz"
}

// MODULE: app-common(lib-common)
fun commonUse(c: A) = c.base()

// MODULE: app-inter1(lib-inter1)()(app-common)
fun inter1Use(c: A) = c.base() + c.bar()

// MODULE: app-inter2(lib-inter2)()(app-inter1)
fun inter2Use(c: A) = c.base() + c.bar() + c.baz()

// MODULE: app-platform(lib-platform)()(app-inter2)
fun box(): String {
    val c = A()
    if (commonUse(c) != "base") return "FAIL"
    if (inter1Use(c) != "basebar") return "FAIL"
    if (inter2Use(c) != "basebarbaz") return "FAIL"
    return "OK"
}
