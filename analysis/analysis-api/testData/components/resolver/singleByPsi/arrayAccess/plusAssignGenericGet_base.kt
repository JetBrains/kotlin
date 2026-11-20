// IGNORE_FIR
class B {
    operator fun <I> get(i: I): B = this
    operator fun set(i: String, value: B) {}
    operator fun plus(other: Int): B = this
}

fun test(b: B) {
    <expr>b["key"]</expr> += 1
}
