// RUN_PIPELINE_TILL: FRONTEND
// See KT-12809
open class A(val a: Any) {
    override fun toString() = a.toString()
}

object B : A(B.<!UNINITIALIZED_VARIABLE!>foo<!>) { // call B.foo should be not-allowed
    val foo = 4
}