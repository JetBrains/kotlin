// IDE_MODE
// LANGUAGE: -ContextSensitiveResolutionUsingExpectedType
package test

sealed class MySealedClass {
    data object SubObject : MySealedClass()
}

fun expectsMySealedClass(s: MySealedClass) {}

fun test() {
    expectsMySealedClass(<expr>MySealedClass.SubObject</expr>)
}
