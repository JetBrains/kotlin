object X1
object X2

class Inv<T>

fun dynamic.foo() = X1
fun <T> Inv<T>.foo() = X2

fun test(): X2 = Inv<Any>().foo()
