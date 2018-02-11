// !WITH_NEW_INFERENCE
val x = ""

fun bar(x : Int = <!TYPE_MISMATCH!>""<!>, y : Int = x, <!UNUSED_PARAMETER!>z<!> : String = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>y<!>) {

}

// KT-371 Resolve default parameters for constructors

class A(x : Int = <!UNINITIALIZED_PARAMETER!>y<!>, y : Int = x) { // None of the references is resolved, no types checked
    fun foo(<!UNUSED_PARAMETER!>bool<!>: Boolean, a: Int = <!NI;TYPE_MISMATCH, TYPE_MISMATCH, UNINITIALIZED_PARAMETER!>b<!>, b: String = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>a<!>) {}
}

val z = 3

fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int = x, <!UNUSED_PARAMETER!>i<!> : Int = z): Int = x + y
