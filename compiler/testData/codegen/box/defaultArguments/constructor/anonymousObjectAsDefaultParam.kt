// ISSUE: KT-74739
// IGNORE_NATIVE: compatibilityTestMode=FORWARD
// ^^^ KT-74739: This new test fails under 2.1.0 compiler and passes on 2.2.0 and later

// FILE: A.kt
open class FooA(val string: String)

class BarA(val foo: FooA? = object: FooA("A") {})

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

class BarC(val foo: FooC? = object: FooC("C") {})
