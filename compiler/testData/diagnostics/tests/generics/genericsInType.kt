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

    Foo<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>::class
    Foo<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><String><!>.Bar.Baz::class

    a<Foo<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>>()
    a<Foo<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><String><!>.Bar.Baz>()

    a<Foo.Bar<Int>>()
    a<Foo.Bar<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.Baz>()
}

fun <T: Foo<String.<!UNRESOLVED_REFERENCE!>Bar<!>>> x() {}
fun Foo<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>.ext() {}

fun ex1(<!UNUSED_PARAMETER!>a<!>: Foo<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><String><!>.Bar<String>): Foo<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><String><!>.Bar<String> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>