// See also: KT-3743
fun foo(arg: Boolean?): String {
    // Must be NOT exhaustive
    return when(arg) {
        true -> "truth"
        false -> "falsehood"
    }
}
