val String.<caret>property: Int
    get() = length * 2

fun String.foo() {
    println("a".property)
    println(property)
}