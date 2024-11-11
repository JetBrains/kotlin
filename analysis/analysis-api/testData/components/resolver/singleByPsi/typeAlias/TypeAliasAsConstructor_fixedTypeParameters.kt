// IGNORE_STABILITY_K2: call
// IGNORE_STABILITY_K1: call
package test

class MyClass<T>(t: T)

class Other

typealias MyAlias = MyClass<Other>

fun usage() {
    <caret>MyAlias(Other())
}