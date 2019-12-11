open class RecA<T>: RecB<T>()
open class RecB<T>: <!OTHER_ERROR!>RecA<T><!>()
open class SelfR<T>: <!OTHER_ERROR!>SelfR<T><!>()

fun test(f: SelfR<String>) = f is RecA<String>
fun test(f: RecB<String>) = f is RecA<String>