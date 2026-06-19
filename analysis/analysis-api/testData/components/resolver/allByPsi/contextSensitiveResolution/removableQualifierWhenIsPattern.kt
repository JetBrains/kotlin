// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class Base {
    class Child : Base()
}

fun usage(b: Base) {
    when (b) {
        is Base.Child -> "".hashCode()
    }
}
