// !DIAGNOSTICS: -UNUSED_EXPRESSION


class Foo<T> {
    class Bar<X> {
        class Baz {

        }
    }
}

fun <T> a() {}

fun test() {
    Foo::class
    Foo.Bar::class
    Foo.Bar.Baz::class

    a<Foo.Bar<String>>()
    a<Foo.Bar.Baz>()

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Foo<String>.Bar::class<!>
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<String>.Bar.Baz<!>::class

    a<Foo<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>>()
    a<Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>.Bar.Baz>()

    a<Foo.Bar<Int>>()
    a<Foo.Bar<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>.Baz>()
}

fun <T: Foo<<!UNRESOLVED_REFERENCE!>String.Bar<!>>> x() {}
fun Foo<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>.ext() {}

fun ex1(a: Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>.Bar<String>): Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>.Bar<String> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
