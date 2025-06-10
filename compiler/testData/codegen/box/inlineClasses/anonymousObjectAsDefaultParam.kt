// ISSUE: KT-61258
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// IGNORE_NATIVE: compatibilityTestMode=NewArtifactOldCompiler
// ^^^ This new test fails in 2.1.0 compiler backend and passes on 2.2.0 and later

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
