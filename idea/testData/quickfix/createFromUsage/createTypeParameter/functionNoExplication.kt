// "Create type parameter 'X'" "true"
fun foo(x: <caret>X) {

}

fun test() {
    foo(1)
    foo("2")
}