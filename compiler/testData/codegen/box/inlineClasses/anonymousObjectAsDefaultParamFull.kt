// ISSUE: KT-61258
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

// MODULE: lib
// FILE: lib.kt
open class Foo(val string: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Bar(val foo: Foo? = object: Foo("O") {})

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val o = Bar().foo?.string
    val k = Bar(Foo("K")).foo?.string
    return o + k
}
