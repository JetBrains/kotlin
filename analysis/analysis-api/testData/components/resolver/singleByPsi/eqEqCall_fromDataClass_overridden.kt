data class D(val id: String) {
    override fun equals(other: Any?): Boolean {
        return (other as? D)?.id == id
    }
}

fun test(d1: D, d2: D) {
    <expr>d1 == d2</expr>
}