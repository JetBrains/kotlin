data class D(val id: String)

// TODO(KT-54844): need to create KtSymbol for data class synthetic members, such as equals/hashCode/toString
fun test(d1: D, d2: D) {
    <expr>d1 == d2</expr>
}