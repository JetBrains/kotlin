// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// Related issue: KT-28654

fun <K> select(): K = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>run { }<!>

fun test() {
    val x: Int = select()
    val t = select()
}
