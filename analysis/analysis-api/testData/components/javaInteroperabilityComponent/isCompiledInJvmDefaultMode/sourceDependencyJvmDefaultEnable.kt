// MODULE: dependency
// JVM_DEFAULT_MODE: enable
// FILE: Foo.kt
interface Foo {
    fun foo() {}
}

// MODULE: main(dependency)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt
fun usage(value: <expr>Foo</expr>) {}
