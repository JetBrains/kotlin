// "Change return type of enclosing function 'foo' to 'Int'" "true"
fun foo(n: Int): Boolean {
    n.let {
        return <caret>1
    }
}