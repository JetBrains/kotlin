// "Change return type of current function 'foo' to 'Int'" "true"
fun foo(n: Int): Boolean {
    n.let {
        return@foo <caret>1
    }
}