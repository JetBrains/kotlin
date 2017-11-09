// "Change return type of enclosing function 'foo' to 'Int'" "true"
// WITH_RUNTIME
fun foo(n: Int): Boolean {
    n.let {
        return@foo <caret>1
    }
}