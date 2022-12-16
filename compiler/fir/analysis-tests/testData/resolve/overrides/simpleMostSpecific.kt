// !CHECK_TYPE
interface A {
    val x: CharSequence

    fun foo(): String?

    fun <T, F> bar(x: T, y: F): F?
}

interface B {
    var x: String

    fun foo(): String

    fun <X, Y> bar(x: X, y: Y): Y
}

interface C {
    val x: String
}

interface D1 : A, B
interface D2 : B, A
interface D3 : A, C

fun main(d1: D1, d2: D2, d3: D3) {
    d1.x.checkType { _<String>() }
    d1.x = ""
    d1.foo().checkType { _<String>() }
    d1.bar(1, "").checkType { _<String>() }

    d2.x.checkType { _<String>() }
    d2.x = ""
    d2.foo().checkType { _<String>() }
    d2.bar(1, "").checkType { _<String>() }

    d3.x.checkType { _<String>() }
    d3.<!VAL_REASSIGNMENT!>x<!> = ""
    d3.foo().checkType { _<String?>() }
    d3.bar(1, "").checkType { _<String?>() }
}
