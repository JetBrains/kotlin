// !LANGUAGE: +JvmFieldInInterface +NestedClassesInAnnotations
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: Foo.kt

public class Bar(public val value: String)

annotation class Foo {
    companion object {
        @JvmField
        val FOO = Bar("OK")
    }
}

// MODULE: main(lib)
// FILE: bar.kt

fun box(): String {
    return Foo.FOO.value
}
