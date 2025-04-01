// ISSUE: KT-61258
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST

// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

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
