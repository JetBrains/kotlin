// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST, -FINAL_UPPER_BOUND
// ISSUE: KT-64645

class Box(val value: String)
typealias NullableBox = Box?

fun <T : NullableBox> foo(x: T) {
    if (x != null) {
        x.value
    }
}

fun foo2(x: NullableBox) {
    if (x != null) {
        x.value
    }
}
