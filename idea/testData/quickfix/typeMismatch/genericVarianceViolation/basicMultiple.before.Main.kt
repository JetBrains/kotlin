// "Cast expression 'x' to 'List<Any>?'" "true"
// ERROR: Java type mismatch expected kotlin.(Mutable)List<kotlin.Any!>! but found kotlin.MutableList<kotlin.String>. Use explicit cast

fun main(x: MutableList<String>) {
    A.foo(<caret>x)
}
