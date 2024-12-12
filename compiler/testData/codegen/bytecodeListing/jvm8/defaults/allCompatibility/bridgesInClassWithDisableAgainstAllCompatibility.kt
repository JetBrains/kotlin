// MODULE: lib
// JVM_DEFAULT_MODE: all-compatibility
// FILE: lib.kt

interface A<T> {
    fun f(t: T): T = t
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt

class C : A<String>
