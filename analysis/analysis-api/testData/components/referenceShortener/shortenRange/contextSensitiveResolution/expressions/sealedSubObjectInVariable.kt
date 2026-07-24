// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class MySealedClass {
    data object SubObject : MySealedClass()
}

fun test() {
    val s: MySealedClass = <expr>MySealedClass.SubObject</expr>
}
