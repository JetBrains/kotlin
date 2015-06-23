package test

import library.LibraryClass

fun LibraryClass./*rename*/foo(a: Int, b: Int) {
}

fun LibraryClass.foo(a: Int) {
}


fun Any.foo() { // won't be renamed
}

fun foo() { // won't be renamed
}
