val x = ""

fun bar(x : Int = "", y : Int = x, z : String = y) {

}

// KT-371 Resolve default parameters for constructors

class A(x : Int = <!UNINITIALIZED_PARAMETER!>y<!>, y : Int = x) { // None of the references is resolved, no types checked
    constructor(x : Int = <!UNINITIALIZED_PARAMETER!>x<!>) : this(x, x)
    fun foo(bool: Boolean, a: Int = <!UNINITIALIZED_PARAMETER!>b<!>, b: String = a) {}
}

val z = 3

fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int = x, i : Int = z): Int = x + y

fun foo(x: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!> }, y: Int = x(), i : Int = z): Int = x() + y

fun bar(x: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!>; 1 }, y: Int) {}

fun baz(
    x: () -> Int = {
        fun bar(xx: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!>; 1 }) = xx
        bar()()
    },
    y: Int
) {
}

fun boo(
    x: () -> Int = {
        fun bar(): Int = <!UNINITIALIZED_PARAMETER!>y<!>
        bar()
    },
    y: Int
) {
}
