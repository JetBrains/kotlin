// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class A<T> {
    fun foo(x: T): T
}

// MODULE: lib-inter()()(lib-common)
typealias AInter<T> = A<T>

// MODULE: lib-platform()()(lib-inter)
actual class A<T> {
    actual fun foo(x: T): T = x
}

// MODULE: app-common(lib-common)
fun useA(a: A<String>): String {
    return a.foo("OK")
}

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun useAInter(a: AInter<String>): String {
    return a.foo("OK")
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String {
    return "OK"
}