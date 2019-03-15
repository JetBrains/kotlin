// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// Related issue: KT-28654

fun <K> select(): K = <!NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>run { <!OI;TYPE_MISMATCH!><!>}<!>

fun test() {
    val x: Int = select()
    <!NI;UNREACHABLE_CODE!>val t =<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>()
}