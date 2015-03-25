// "Change 'foo' function return type to 'Int'" "true"
fun foo(n: Int): Int {
    n.let {
        return@foo 1
    }
}