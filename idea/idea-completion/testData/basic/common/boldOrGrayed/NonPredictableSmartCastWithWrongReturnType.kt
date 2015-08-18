interface A {
    fun foo(): Any
    fun bar()
}
interface B: A {
    override fun foo(): String
}

fun f(pair: Pair<out A, out Any>) {
    if (pair.first !is B) return
    pair.first.<caret>
}
/*TODO: { lookupString: "foo", "typeText":"Any", attributes: "bold" } */
// EXIST: { lookupString: "foo", "typeText":"String", attributes: "bold" }
// EXIST: { lookupString: "bar", attributes: "bold" }
