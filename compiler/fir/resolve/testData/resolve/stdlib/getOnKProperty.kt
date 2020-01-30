val Any?.meaning: Int
    get() = 42

fun test() {
    val f = Any?::meaning
    f.<!INAPPLICABLE_CANDIDATE!>get<!>(null)
    f.get("")
}