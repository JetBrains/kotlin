// ISSUE: KT-72356
// STOP_EVALUATION_CHECKS
// FILE: A.kt
annotation class A(val x: String)

@kotlin.annotation.Target(kotlin.annotation.AnnotationTarget.TYPE_PARAMETER)
annotation class Something

// FILE: B.kt
open class B { @A(<!EVALUATED("String  ")!>"String  "<!>) fun foo() {} }

// FILE: E.kt
fun <          @Something X> bar() {}

// E has a fake override of foo(), which has annotation with const param having SAME source range as @Something, but in ANOTHER source file
class E : B()

fun box(): String {
    return "OK"
}
