// COMPARE_WITH_LIGHT_TREE
data class Foo(val name: String)

fun main() {
    val foo = Foo("John")
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT{PSI}!>val (x) = foo<!>) {
        <!USELESS_IS_CHECK!>is String<!> -> bar("1")
        <!USELESS_IS_CHECK!>is Foo<!> -> bar("2")
        else -> bar(<!UNRESOLVED_REFERENCE!>x<!>)
    }
}

fun main2() {
    val foo = Foo("John")
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT{PSI}!>val (x) = foo<!>) {
        <!USELESS_IS_CHECK!>is String<!> -> bar("1")
        <!USELESS_IS_CHECK!>is Foo<!> -> bar("2")
    }
}

fun bar(x: Any) {}