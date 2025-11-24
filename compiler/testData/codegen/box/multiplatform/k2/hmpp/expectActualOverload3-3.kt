// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect open class LibA()

fun foo(a: LibA): String = "libCommon"

// MODULE: lib-inter()()(lib-common)
fun foo(a: LibA, b: Int): String = "libInter$b"

// MODULE: lib-platform()()(lib-inter)
actual open class LibA actual constructor()

fun foo(a: LibA, t: String): String = "libPlatform$t"

// MODULE: app-common(lib-common)
class AppA : LibA()

fun foo(a: AppA, d: Double): String = "app$d"

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun appInterUse(a: AppA): String {
    val r1 = foo(a as LibA)
    val r2 = foo(a as LibA, 1)
    val r3 = foo(a, 1.5)
    return "$r1$r2$r3"
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun testOverload(a: AppA): String {
    val fromInter = appInterUse(a)
    val r4 = foo(a as LibA, "x")
    return "$fromInter$r4"
}

fun box(): String {
    val a = AppA()
    val r = testOverload(a)
    return if (r == "libCommonlibInter1app1.5libPlatformx") "OK" else "FAIL"
}
