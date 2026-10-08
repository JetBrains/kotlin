// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-89461
// MODULE: lib-common
interface Base<A> {
    fun foo(x: A) {}
}

interface Derived<B> : Base<B>
interface Derived2 : Derived<String>

class Impl : Base<String>, Derived2

// MODULE: lib-platform()()(lib-common)

// MODULE: app-common(lib-common)
fun test_common(lc1: Impl) {}

// MODULE: app-platform(lib-platform)()(app-common)
fun test_platform() {}

fun box(): String {
    return "OK"
}
