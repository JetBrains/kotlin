// IGNORE_BACKEND_FIR: JVM_IR
interface In<in E>
class A : In<A>
class B : In<B>
fun <T> select(x: T, y: T) = x ?: y

// This test just checks that no internal error happens in backend
fun foobar(a: A, b: B) = select(a, b)

fun box() = "OK"
