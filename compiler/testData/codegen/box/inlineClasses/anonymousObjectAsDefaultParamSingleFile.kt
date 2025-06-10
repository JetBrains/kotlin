// ISSUE: KT-61258
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// IGNORE_NATIVE: compatibilityTestMode=NewArtifactOldCompiler
// ^^^ This new test fails in 2.1.0 compiler backend and passes on 2.2.0 and later

// FILE: A.kt
open class FooA(val string: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class BarA(val foo: FooA? = object: FooA("A") {})

// FILE: B.kt
fun box(): String {
    val a = BarA().foo?.string
    val b = BarA(FooA("B")).foo?.string
    val ab = a + b
    if (ab != "AB") return ab

    val c = BarC().foo?.string
    val d = BarC(FooC("D")).foo?.string
    val cd = c + d
    if (cd != "CD") return cd
    return "OK"
}

// FILE: C.kt
open class FooC(val string: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class BarC(val foo: FooC? = object: FooC("C") {})
