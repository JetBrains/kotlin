// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

open class A

class B : A()

fun ff(l: MutableCollection<B>) = l is MutableList<out A>

