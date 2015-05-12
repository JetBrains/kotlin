// See also: KT-3743
fun foo(arg: Boolean?): String {
    // Must be NOT exhaustive
    return <!NO_ELSE_IN_WHEN!>when<!>(arg) {
        true -> "truth"
        false -> "falsehood"
    }
}
