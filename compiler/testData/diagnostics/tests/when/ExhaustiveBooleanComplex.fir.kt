// See also: KT-3743
fun foo(arg: Boolean): String {
    // Must be exhaustive
    return when(arg) {
        2 == 2 -> "truth"
        2 == 1 -> "falsehood"
    }
}
