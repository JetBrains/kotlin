// WITH_STDLIB
// IGNORE_FE10

class A<T>(val t: T) {
    var x: A<Int> by f<caret>oo
}
