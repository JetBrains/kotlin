// MODULE: dependency
// JVM_DEFAULT_MODE: disable
// FILE: Foo.kt
interface Foo {
    fun foo() {}
}

// MODULE: main(dependency)
// FILE: main.kt
fun usage(value: <expr>Foo</expr>) {}
