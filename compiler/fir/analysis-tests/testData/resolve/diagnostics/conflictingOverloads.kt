// RUN_PIPELINE_TILL: FRONTEND
<!CONFLICTING_OVERLOADS!>fun test(x: Int)<!> {}

<!CONFLICTING_OVERLOADS!>fun test(y: Int)<!> {}

fun test() {}

fun test(z: Int, c: Char) {}

open class <!CLASSIFIER_REDECLARATION!>A<!> {
    open fun rest(s: String) {}

    open val u = 20
}

class <!CLASSIFIER_REDECLARATION!>A<!> {

}

class <!CLASSIFIER_REDECLARATION!>B<!> : <!SUPERTYPE_NOT_INITIALIZED!>A<!> {
    override <!CONFLICTING_OVERLOADS!>fun rest(s: String)<!> {}

    <!CONFLICTING_OVERLOADS!>fun <!VIRTUAL_MEMBER_HIDDEN!>rest<!>(s: String)<!> {}

    fun rest(l: Long) {}

    override val u = 310
}

interface <!CLASSIFIER_REDECLARATION!>B<!>

enum class <!CLASSIFIER_REDECLARATION!>B<!>

val <!REDECLARATION!>u<!> = 10
val <!REDECLARATION!>u<!> = 20

val <!SYNTAX!>(a,b)<!> = 30 to 40
val <!SYNTAX!>(c,d)<!> = 50 to 60

typealias <!CLASSIFIER_REDECLARATION!>TA<!> = A
typealias <!CLASSIFIER_REDECLARATION!>TA<!> = B

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
    val <!REDECLARATION!>Companion<!> = object : Any() {}
}

fun B.foo() {}

class L {
    fun B.foo() {}
}

<!CONFLICTING_OVERLOADS!>fun mest()<!> {}

class <!CONFLICTING_OVERLOADS!>mest<!>

<!FUNCTION_DECLARATION_WITH_NO_NAME!>fun()<!> {}

private <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun()<!> {}
