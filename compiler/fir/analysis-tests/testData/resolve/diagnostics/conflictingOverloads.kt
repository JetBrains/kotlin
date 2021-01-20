<!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS{PSI}!>fun test(x: Int)<!> {}<!>

<!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS{PSI}!>fun test(y: Int)<!> {}<!>

fun test() {}

fun test(z: Int, c: Char) {}

<!REDECLARATION!>open class A {
    open fun rest(s: String) {}

    open val u = 20
}<!>

<!REDECLARATION!>class A {

}<!>

<!REDECLARATION!>class B : A {
    <!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS{PSI}!>override fun rest(s: String)<!> {}<!>

    <!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS{PSI}!>fun rest(s: String)<!> {}<!>

    fun rest(l: Long) {}

    override val u = 310
}<!>

<!REDECLARATION!>interface B<!>

<!REDECLARATION!>enum class B<!>

<!REDECLARATION!>val u = 10<!>
<!REDECLARATION!>val u = 20<!>

<!REDECLARATION!>typealias TA = A<!>
<!REDECLARATION!>typealias TA = B<!>

typealias BA = A

fun <T> kek(t: T) where T : (String) -> Any?, T : Char {}
fun <T> kek(t: T) where T : () -> Boolean, T : String {}
fun <T : Int> kek(t: T) {}

fun lol(a: Array<Int>) {}
fun lol(a: Array<Boolean>) {}

<!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS{PSI}!>fun <T> mem(t: T)<!> where T : () -> Boolean, T : String {}<!>
<!CONFLICTING_OVERLOADS{LT}!><!CONFLICTING_OVERLOADS{PSI}!>fun <T> mem(t: T)<!> where T : String, T : () -> Boolean {}<!>

class M {
    <!REDECLARATION{LT}!>companion <!REDECLARATION{PSI}!>object<!> {}<!>
    <!REDECLARATION!>val Companion = object : Any {}<!>
}

fun B.foo() {}

class L {
    fun B.foo() {}
}

fun mest()

class mest

fun() {}

private fun() {}
