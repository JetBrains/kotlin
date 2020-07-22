//KT-304: Resolve supertype reference to class anyway

open class Foo() : <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Bar<!>() {
}

open class Bar<T>() {
}