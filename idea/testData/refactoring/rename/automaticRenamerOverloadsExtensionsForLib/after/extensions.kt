package test

import library.LibraryClass

fun LibraryClass./*rename*/bar(a: Int, b: Int) {
}

fun LibraryClass.bar(a: Int) {
}


fun Any.foo() { // won't be renamed
}

fun foo() { // won't be renamed
}
