// PROBLEM: none
fun foo(): Int? = null

fun test() : Int? {
    val i = foo() <caret>?: return null
    return 0
}