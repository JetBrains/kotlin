// See also: KT-3743
fun foo(arg: Boolean): String {
    // Must be exhaustive
    return when(arg) {
        true -> "truth"
        false -> "falsehood"
    }
}
