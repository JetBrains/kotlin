// !DIAGNOSTICS: -UNUSED_VARIABLE
// Related issue: KT-28654

fun <K> select(): K = <!TYPE_MISMATCH!>run <!TYPE_MISMATCH!>{ }<!><!>

fun test() {
    val x: Int = select()
    val t = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>()
}
