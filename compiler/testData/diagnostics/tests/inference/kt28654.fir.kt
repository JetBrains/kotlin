// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// Related issue: KT-28654

fun <K> select(): K = run { }

fun test() {
    val x: Int = select()
    val t = select()
}