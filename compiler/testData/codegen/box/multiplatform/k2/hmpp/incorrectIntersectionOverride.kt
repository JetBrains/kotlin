// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-89461
// MODULE: lib-common
interface Base<A> {
    fun foo(x: A) {}
}

interface Derived<B> : Base<B>

class Impl<C> : Base<C>, Derived<C>

// MODULE: lib-platform()()(lib-common)

// MODULE: app-common(lib-common)
fun test_common(lc1: Impl<*>) {}

// MODULE: app-platform(lib-platform)()(app-common)
fun test_platform() {}

fun box(): String {
    return "OK"
}
