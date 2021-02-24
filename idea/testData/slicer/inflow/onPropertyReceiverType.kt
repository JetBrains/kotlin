// FLOW: IN

var <caret>Any.property: Int
    get() = 1
    set(value) { }

fun foo() {
    val v = "a".property
    "b".property = 2
}