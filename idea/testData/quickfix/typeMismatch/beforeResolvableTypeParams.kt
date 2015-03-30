// "Change parameter 'n' type of function 'foo' to 'T'" "true"
fun bar<T>(t: T) {
    fun foo(n: Int) {

    }

    foo(<caret>t)
}