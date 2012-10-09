trait A<O, K> {
    val o: O
    val k: K
}

trait B<K> : A<String, K>

trait C<O> : A<O, String>

class D : B<String>, C<String> {
    override val o = "O"
    override val k = "K"
}

fun box(): String {
    val a: A<String, String> = D()
    return a.o + a.k
}
