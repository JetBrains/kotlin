// ISSUE: KT-61258
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

// MODULE: lib
// FILE: lib.kt
open class Foo

OPTIONAL_JVM_INLINE_ANNOTATION
value class Bar(val foo: Foo? = object: Foo() {})

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    Bar(null)
    return "OK"
}
