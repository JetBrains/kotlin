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

    <!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED!>Foo<String>.Bar<!>::class
    <!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED!>Foo<<!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>>.Bar.Baz<!>::class

    a<<!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<String>.Bar<!>>()
    a<<!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED!>Foo<<!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>>.Bar.Baz<!>>()

    a<Foo.Bar<Int>>()
    a<<!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED!>Foo.Bar<Int>.Baz<!>>()
}

fun <T: Foo<String.<!UNRESOLVED_REFERENCE!>Bar<!>>> x() {}
fun <!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<String>.Bar<!>.ext() {}

fun ex1(<!UNUSED_PARAMETER!>a<!>: <!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED!>Foo<String>.Bar<String><!>): <!GENERICS_IN_CONTAINING_TYPE_NOT_ALLOWED!>Foo<String>.Bar<String><!> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>