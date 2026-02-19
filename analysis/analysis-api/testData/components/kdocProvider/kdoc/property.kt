// WITH_STDLIB
// MODULE: main
// FILE: main.kt

/**
 * Foo KDoc
 *
 * @property a property
 * @property b property
 */
class Foo(val a: String) {
    val b: String
}

/**
 * Bar KDoc
 *
 * @param a property
 * @param b property
 */
class Bar(var a: String) {
    var b: String
}