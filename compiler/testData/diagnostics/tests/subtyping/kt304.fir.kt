//KT-304: Resolve supertype reference to class anyway

open class Foo() : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Bar<!>() {
}

open class Bar<T>() {
}