// ISSUE: KT-63654

class Klass<in A>(private val action: (A) -> Unit) {
    fun <B> execute(value: B, klassB: Klass<B>) {
        klassB.action(value)
    }
}

fun box(): String {
    Klass { a: Int -> a.inc() }.execute("", Klass { b: String -> b.length })
    return "OK"
}
