// TARGET_BACKEND: NATIVE
// ISSUE: KT-67224

// MODULE: lib
// FILE: lib.kt

interface Foo<T> {
    @Throws(IllegalArgumentException::class)
    public fun f(data: T) = data
}

// MODULE: main(lib)
// FILE: main.kt

class Bar<K> : Foo<K> {
    @Throws(IllegalArgumentException::class)
    override fun f(data: K) = data
}

fun box() = Bar<String>().f("OK")