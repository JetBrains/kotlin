// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

expect class E
expect fun foo(e: E): String

// MODULE: lib-inter()()(lib-common)

fun libInterFoo(e: E): String = foo(e)

// MODULE: lib-platform()()(lib-inter)

actual class E

@Deprecated("", level = DeprecationLevel.WARNING)
actual fun foo(e: E): String = "OK"

// MODULE: app-common(lib-common)

fun appCommonFoo(e: E): String = foo(e)


// MODULE: app-inter(lib-inter)(lib-common)(app-common)

fun appInterFoo(e: E): String =
    libInterFoo(e) + appCommonFoo(e)

// MODULE: app-platform(lib-platform)()(app-inter)

fun box(): String {
    val e = E()
    val result = appInterFoo(e)
    return if (result == "OKOK") "OK" else "FAIL"
}
