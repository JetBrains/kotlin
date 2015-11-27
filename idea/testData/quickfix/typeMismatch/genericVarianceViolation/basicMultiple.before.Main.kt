// "Cast expression 'x' to 'List<Any>?'" "true"
// ERROR: Java type mismatch expected kotlin.collections.(Mutable)List<kotlin.Any!>! but found kotlin.collections.MutableList<kotlin.String>. Use explicit cast

fun main(x: MutableList<String>) {
    A.foo(<caret>x)
}
