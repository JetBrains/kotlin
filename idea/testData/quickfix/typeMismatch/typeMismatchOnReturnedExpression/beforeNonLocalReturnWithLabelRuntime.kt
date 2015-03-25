// "Change 'foo' function return type to 'Int'" "true"
fun foo(n: Int): Boolean {
    n.let {
        return@foo <caret>1
    }
}