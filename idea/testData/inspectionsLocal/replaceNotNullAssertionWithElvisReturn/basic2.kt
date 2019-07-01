fun foo(): Int? = null

fun test() {
    foo()!!<caret>
}