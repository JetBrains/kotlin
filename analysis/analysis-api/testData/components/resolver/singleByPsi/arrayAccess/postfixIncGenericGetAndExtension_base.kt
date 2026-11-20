// IGNORE_FIR
class B {
    operator fun <I> get(i: I): B = this
    operator fun set(i: String, value: B) {}
    operator fun inc(): B = this
}

operator fun B.get(i: CharSequence): B = this

fun test(b: B) {
    <expr>b["key"]</expr>++
}
