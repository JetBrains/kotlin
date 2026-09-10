// LANGUAGE: +MultiPlatformProjects
// IGNORE_HMPP: ANY
// ISSUE: KT-89341

// MODULE: lib-common
class Some<E> {
    inner class Inner<T> {
        fun add(e: E, x: T) {}
    }
}

// MODULE: lib-platform()()(lib-common)

// MODULE: app-common(lib-common)
fun test(s: Some<Int?>.Inner<String?>) {
    s.add(null, null)
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    return "OK"
}
