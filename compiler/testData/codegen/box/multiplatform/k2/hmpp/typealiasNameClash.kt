// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect class Lib1A

// MODULE: lib-inter()()(lib-common)

typealias AInter1 = Lib1A

// MODULE: lib-platform()()(lib-inter)

actual typealias Lib1A = String
typealias A = Lib1A

fun libPlatformFoo(a: A, b: String): A = a + b


// MODULE: lib2-common

class Lib2(val v: Int)

// MODULE: lib2-inter()()(lib2-common)

// MODULE: lib2-platform()()(lib2-inter)

typealias A = String

fun lib2PlatformFoo(a: A): A = "$a"


// MODULE: app-common(lib-common)

fun appCommonUse(x: Lib1A): String = x.toString()

// MODULE: app-inter(lib2-inter)(lib2-common)(app-common)

fun appInterUse(x: Lib2): Int = x.v

// MODULE: app-platform(lib-platform, lib2-platform)()(app-inter)

fun box(): String {
    val a: A = "OK"
    val c1 = libPlatformFoo(a, "")
    val c2 = lib2PlatformFoo(c1)
    return if (c2.contains("OK")) "OK" else "FAIL"
}
