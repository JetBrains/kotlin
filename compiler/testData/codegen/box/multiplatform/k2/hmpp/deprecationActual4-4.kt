// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class E
expect fun foo(e: E): String

// MODULE: lib-inter1()()(lib-common)
fun inter1Foo(e: E): String = foo(e)

// MODULE: lib-inter2()()(lib-common)
fun inter2Foo(e: E): String = foo(e)

// MODULE: lib-platform()()(lib-inter1, lib-inter2)
actual class E

@Deprecated("", level = DeprecationLevel.WARNING)
actual fun foo(e: E): String = "OK"

// MODULE: app-common(lib-common)
fun appCommonFoo(e: E): String = foo(e)

// MODULE: app-inter1(lib-inter1)(lib-common)(app-common)
fun appInterFoo1(e: E): String =
    inter1Foo(e) + appCommonFoo(e)

// MODULE: app-inter2(lib-inter2)(lib-common)(app-common)
fun appInterFoo2(e: E): String =
    inter2Foo(e) + appCommonFoo(e)

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
fun box(): String {
    val e = E()
    val r1 = appInterFoo1(e)
    val r2 = appInterFoo2(e)
    val r3 = r1 + r2
    return if (r3 == "OKOKOKOK") "OK" else "FAIL"
}
