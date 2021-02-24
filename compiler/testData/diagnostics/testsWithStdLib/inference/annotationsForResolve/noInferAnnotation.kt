//!DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> test1(t1: T, t2: @kotlin.internal.NoInfer T): T = t1

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> @kotlin.internal.NoInfer T.test2(t1: T): T = t1

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> test3(t1: @kotlin.internal.NoInfer T): T = t1

fun usage() {
    <!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>test1<!>(1, <!TYPE_MISMATCH!>"312"<!>)
    <!TYPE_MISMATCH{OI}!>1<!>.<!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>test2<!>("")
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>test3<!>("")
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> List<T>.contains1(e: @kotlin.internal.NoInfer T): Boolean = true

fun test(i: Int?, a: Any, l: List<Int>) {
    l.<!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>contains1<!>(<!TYPE_MISMATCH!>a<!>)
    l.<!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>contains1<!>(<!TYPE_MISMATCH!>""<!>)
    l.<!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>contains1<!>(<!TYPE_MISMATCH!>i<!>)
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> assertEquals1(e1: T, e2: @kotlin.internal.NoInfer T): Boolean = true

fun test(s: String) {
    <!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>assertEquals1<!>(s, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>11<!>)
}
