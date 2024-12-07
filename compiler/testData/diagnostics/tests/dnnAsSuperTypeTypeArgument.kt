// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class A<TA>

class B<TB> : A<TB & Any>()

fun accept(a: A<String>) {}

fun test() {
    val b = B<String?>()
    accept(b)
}