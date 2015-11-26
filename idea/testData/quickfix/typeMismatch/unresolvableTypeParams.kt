// "Change parameter 'n' type of function 'foo' to 'Any?'" "true"
fun foo(n: Int) {

}

fun <T> bar(t: T) {
    foo(<caret>t)
}