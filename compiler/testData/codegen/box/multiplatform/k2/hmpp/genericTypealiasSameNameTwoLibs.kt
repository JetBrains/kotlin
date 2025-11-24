// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
class Lib1(val value: String)

// MODULE: lib-inter()()(lib-common)
open class LibInterBase<T>(val t: List<T>)

// MODULE: lib-platform()()(lib-inter)
typealias A<T> = List<T>

class LibPlatformFoo<T>(val a: A<T>) : LibInterBase<T>(a) {
    fun foo(): T = a.first()
}

// MODULE: lib2-common
class Lib2(val n: Int)

// MODULE: lib2-inter()()(lib2-common)
open class Lib2InterBase<T>(val t: List<T>)

// MODULE: lib2-platform()()(lib2-inter)
typealias A<T> = List<T>

class Lib2PlatformFoo<T>(val a: A<T>) : Lib2InterBase<T>(a) {
    fun foo(): T = a.last()
}

// MODULE: app-common(lib-common)
fun appCommonUse(l: Lib1): String = l.value

// MODULE: app-inter(lib2-inter)(lib2-common)(app-common)
fun appInterUse(l: Lib2): Int = l.n

// MODULE: app-platform(lib-platform, lib2-platform)()(app-inter)

fun box(): String {
    val list: A<String> = listOf("OK", "KO")

    val b1 = LibPlatformFoo(list)
    val b2 = Lib2PlatformFoo(list)

    val h = b1.foo()
    val t = b2.foo()

    return if (h == "OK" && t == "KO") "OK" else "FAIL"
}
