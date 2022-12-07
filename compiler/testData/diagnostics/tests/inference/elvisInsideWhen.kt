// FIR_IDENTICAL
// SKIP_TXT

fun <X> select(vararg x: X): X = x[0]
fun <E> myOut(): Out<E> = TODO()

interface Out<out T>

fun foo(x: Int, o: Out<String>, oNullable: Out<String>?) {
    val y: Out<*> = when {
        x > 0 -> when {
            x == 1 -> o
            // Once elvis is being analyzed with expected type Out<*>, because of the @Exact annotation it sticks to that type
            // While here, it's better to use the Out<String> type from the main branch
            else -> oNullable ?: myOut()
        }
        else -> myOut<Any?>()
    }
}
