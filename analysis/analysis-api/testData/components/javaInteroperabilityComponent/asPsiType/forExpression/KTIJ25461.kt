interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> isEqualTo(expected: B) : T
}

fun test(b: BodySpec<String, *>) {
    b.<expr>isEqualTo</expr>("")
}