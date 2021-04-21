// !WITH_NEW_INFERENCE
val x = ""

fun bar(x : Int = "", y : Int = x, z : String = y) {

}

// KT-371 Resolve default parameters for constructors

class A(x : Int = <!UNINITIALIZED_PARAMETER!>y<!>, y : Int = x) { // None of the references is resolved, no types checked
    fun foo(bool: Boolean, a: Int = <!UNINITIALIZED_PARAMETER!>b<!>, b: String = a) {}
}

val z = 3

fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int = x, i : Int = z): Int = x + y

fun foo(x: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!> }, y: Int = x(), i : Int = z): Int = x() + y
