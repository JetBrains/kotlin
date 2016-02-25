// "Cast expression 'x' to 'List<Any>?'" "true"
// ERROR: Java type mismatch expected (Mutable)List<Any!>! but found MutableList<String>. Use explicit cast

fun main(x: MutableList<String>) {
    A.foo(<caret>x)
}
