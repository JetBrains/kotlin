// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

fun <T : CharSequence?> bar1(x: T) {}

fun bar2(x: CharSequence?) {}

fun <T : CharSequence> bar3(x: T) {}

fun bar4(x: String) {}

fun <T : String?> foo(x: T) {
    bar1(x)
    bar2(x)

    <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>bar3<!>(<!NI;TYPE_MISMATCH!>x<!>)
    bar4(<!TYPE_MISMATCH!>x<!>)
}
