// IGNORE_BACKEND_FIR: JVM_IR
interface A<O, K> {
    val o: O
    val k: K
}

interface B<K> : A<String, K>

interface C<O> : A<O, String>

class D : B<String>, C<String> {
    override val o = "O"
    override val k = "K"
}

fun box(): String {
    val a: A<String, String> = D()
    return a.o + a.k
}
