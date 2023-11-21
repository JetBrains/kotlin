// ISSUE: KT-72356
// STOP_EVALUATION_CHECKS
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

// FILE: Something.kt
annotation class A(val x: String)

annotation class Something

// FILE: C.kt
open class C { fun foo(@A(<!EVALUATED("SomeWord")!>"SomeWord"<!>) x: Int) {} }

// FILE: D.kt
class D {                 @Something fun bar() {} }

class E : C()

fun box(): String {
    return "OK"
}
