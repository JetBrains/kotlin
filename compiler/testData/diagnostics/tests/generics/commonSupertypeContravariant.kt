// NI_EXPECTED_FILE

interface In<in E>

class En<T> : In<T>
class A : In<A>
fun <T> select(x: T, y: T): T = x ?: y

// Return type should be In<*> nor In<out Any?>
fun foobar(e: En<*>) = select(A(), e)
