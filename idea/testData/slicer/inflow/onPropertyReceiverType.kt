// FLOW: IN

var <caret>Any.property: Int
    get() = 1
    set(value) { }

fun foo() {
    println("a".property)
    "b".property = 2
}