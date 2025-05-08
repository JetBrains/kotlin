// LANGUAGE: +MultiPlatformProjects +ExpectRefinement
// WITH_STDLIB
// IGNORE_HMPP: ANY
// ^KT-77481

// MODULE: lib-common
expect class Some {
    fun foo(): String
}

fun testLibCommon(foo: Some): String {
    return foo.foo()
}

// MODULE: lib-inter()()(lib-common)
@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect class Some {
    fun foo(): String
    fun bar(): String
}

fun testLibIntermediate(s: Some): String {
    return s.foo() + s.bar()
}

// MODULE: lib-platform()()(lib-inter)
actual class Some {
    actual fun foo(): String = "1"
    actual fun bar(): String = "2"
    fun baz(): String = "3"
}

fun testLibPlatform(s: Some): String {
    return s.foo() + s.bar() + s.baz()
}

fun libBox(): String {
    val s = Some()
    testLibCommon(s).takeIf { it != "1" }?.let { return it }
    testLibIntermediate(s).takeIf { it != "12" }?.let { return it }
    testLibPlatform(s).takeIf { it != "123" }?.let { return it }
    return "OK"
}

// MODULE: app-common(lib-common)
fun testAppCommon(foo: Some): String {
    return foo.foo()
}

// MODULE: app-inter(lib-inter)()(app-common)
fun testAppIntermediate(s: Some): String {
    return s.foo() + s.bar()
}

// MODULE: app-platform(lib-platform)()(app-inter)

fun testAppPlatform(s: Some): String {
    return s.foo() + s.bar() + s.baz()
}

fun appBox(): String {
    val s = Some()
    testLibCommon(s).takeIf { it != "1" }?.let { return it }
    testLibIntermediate(s).takeIf { it != "12" }?.let { return it }
    testLibPlatform(s).takeIf { it != "123" }?.let { return it }
    return "OK"
}

fun box(): String {
    libBox()?.takeIf { it != "OK" }?.let { return "Fail lib: $it" }
    appBox()?.takeIf { it != "OK" }?.let { return "Fail app: $it" }
    return "OK"
}
