// ISSUE: KT-63654

class Klass<out A>(private val action: () -> A) {
    fun <B> execute(klassB: Klass<B>) {
        klassB.action()
    }
}

fun box(): String {
    Klass { 42 }.execute(Klass { "" })
    return "OK"
}
