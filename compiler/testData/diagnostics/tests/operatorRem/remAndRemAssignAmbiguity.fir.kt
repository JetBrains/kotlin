// !DIAGNOSTICS: -UNUSED_PARAMETER

object RemAndRemAssign {
    operator fun rem(x: Int) = RemAndRemAssign
}

operator fun RemAndRemAssign.remAssign(x: Int) {}

fun test() {
    var c = RemAndRemAssign
    <!ASSIGN_OPERATOR_AMBIGUITY!>c %= 1<!>
}