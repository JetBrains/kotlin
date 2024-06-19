data class Foo(val name: String)

fun main() {
    val foo = Foo("John")
    when (<!DECLARATION_IN_ILLEGAL_CONTEXT!>val (x) = <!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!>) {
        is String -> bar("1")
        is Foo -> bar("2")
        else -> bar(<!UNRESOLVED_REFERENCE!>x<!>)
    }
}

fun main2() {
    val foo = Foo("John")
    when (<!DECLARATION_IN_ILLEGAL_CONTEXT!>val (x) = <!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!>) {
        is String -> bar("1")
        is Foo -> bar("2")
    }
}

fun bar(x: Any) {}
