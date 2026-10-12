// class: Foo

// MODULE: dependency
// JVM_DEFAULT_MODE: no-compatibility
// FILE: Foo.kt
interface Foo {
    fun foo() {}
}

// MODULE: main(dependency)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt
fun usage(value: Foo) {}
