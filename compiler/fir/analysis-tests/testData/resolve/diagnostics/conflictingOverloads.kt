// FIR_IDE_IGNORE

<!CONFLICTING_OVERLOADS!>fun test(x: Int)<!> {}

<!CONFLICTING_OVERLOADS!>fun test(y: Int)<!> {}

fun test() {}

fun test(z: Int, c: Char) {}

<!REDECLARATION!>open class A {
    open fun rest(s: String) {}

    open val u = 20
}<!>

<!REDECLARATION!>class A {

}<!>

<!REDECLARATION!>class B : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>A<!> {
    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> fun rest(s: String)<!> {}

    <!CONFLICTING_OVERLOADS!>fun rest(s: String)<!> {}

    fun rest(l: Long) {}

    <!NOTHING_TO_OVERRIDE!>override<!> val u = 310
}<!>

<!REDECLARATION!>interface B<!>

<!REDECLARATION!>enum class B<!>

<!REDECLARATION!>val u = 10<!>
<!REDECLARATION!>val u = 20<!>

<!REDECLARATION!>typealias TA = A<!>
<!REDECLARATION!>typealias TA = B<!>

typealias BA = A

fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> kek(t: T) where T : (String) -> Any?, T : <!FINAL_UPPER_BOUND!>Char<!> {}
fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> kek(t: T) where T : () -> Boolean, T : <!FINAL_UPPER_BOUND!>String<!> {}
fun <T : <!FINAL_UPPER_BOUND!>Int<!>> kek(t: T) {}

fun lol(a: Array<Int>) {}
fun lol(a: Array<Boolean>) {}

<!CONFLICTING_OVERLOADS!>fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> mem(t: T)<!> where T : () -> Boolean, T : <!FINAL_UPPER_BOUND!>String<!> {}
<!CONFLICTING_OVERLOADS!>fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> mem(t: T)<!> where T : <!FINAL_UPPER_BOUND!>String<!>, T : () -> Boolean {}

class M {
    companion <!REDECLARATION!>object<!> {}
    <!REDECLARATION!>val Companion = object : Any {}<!>
}

fun B.foo() {}

class L {
    fun B.foo() {}
}

fun mest() {}

class mest

<!FUNCTION_DECLARATION_WITH_NO_NAME!>fun()<!> {}

<!FUNCTION_DECLARATION_WITH_NO_NAME!>private fun()<!> {}
