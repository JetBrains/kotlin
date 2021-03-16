fun foo(u : Unit) : Int = 1

fun test() : Int {
    <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /foo">foo</error>(1)
    val a : () -> Unit = {
        <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /foo">foo</error>(1)
    }
    return 1
}
