// "Change parameter 'n' type of function 'foo' to 'T'" "true"
fun <T> bar(t: T) {
    fun foo(n: Int) {

    }

    foo(<caret>t)
}