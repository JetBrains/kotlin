// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib1-common
expect open class Lib1A()

fun foo(a: Lib1A): String = "lib1Common"

// MODULE: lib1-inter()()(lib1-common)
fun foo(a: Lib1A, b: Int): String = "lib1Inter$b"

// MODULE: lib1-platform()()(lib1-inter)
actual open class Lib1A actual constructor()

fun foo(a: Lib1A, t: String): String = "lib1Platform$t"

// MODULE: lib2-common
expect open class Lib2B()

fun bar(b: Lib2B): String = "lib2Common"

// MODULE: lib2-inter()()(lib2-common)
fun bar(b: Lib2B, f: Int): String = "lib2Inter$f"

// MODULE: lib2-platform()()(lib2-inter)
actual open class Lib2B actual constructor()

fun bar(b: Lib2B, t: String): String = "lib2Platform$t"

// MODULE: app-common(lib1-common, lib2-common)
open class AppA1 : Lib1A()
open class AppB1 : Lib2B()

fun foo(a: AppA1, d: Double): String = "appFoo$d"
fun bar(b: AppB1, d: Double): String = "appBar$d"

// MODULE: app-inter(lib1-inter, lib2-inter)(lib1-common, lib2-common)(app-common)
fun appInterUse(a: AppA1, b: AppB1): String {
    val r1 = foo(a as Lib1A)
    val r2 = foo(a as Lib1A, 1)
    val r3 = foo(a, 1.5)

    val s1 = bar(b as Lib2B)
    val s2 = bar(b as Lib2B, 2)
    val s3 = bar(b, 2.5)

    return "$r1$r2$r3$s1$s2$s3"
}

// MODULE: app-platform(lib1-platform, lib2-platform)()(app-inter)
fun appPlatformUse(a: AppA1, b: AppB1): String {
    val fromInter = appInterUse(a, b)
    val r4 = foo(a as Lib1A, "x")
    val s4 = bar(b as Lib2B, "y")
    return "$fromInter$r4$s4"
}

fun box(): String {
    val a = AppA1()
    val b = AppB1()
    val r = appPlatformUse(a, b)
    val exp = "lib1Commonlib1Inter1appFoo1.5lib2Commonlib2Inter2appBar2.5lib1Platformxlib2Platformy"
    return if (r == exp) "OK" else "FAIL"
}
