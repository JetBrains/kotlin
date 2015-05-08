// "Change parameter 'n' type of function 'foo' to 'Any?'" "true"
fun foo(n: Int) {

}

fun bar<T>(t: T) {
    foo(<caret>t)
}