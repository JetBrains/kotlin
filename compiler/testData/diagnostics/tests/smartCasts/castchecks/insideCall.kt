// !LANGUAGE: +SafeCastCheckBoundSmartCasts
// See KT-19007

// Stub
fun String.indexOf(arg: String) = this.length - arg.length

// Stub
fun String.toLowerCase() = this

fun foo(a: Any) {
    // Should compile in 1.2
    (a as? String)?.indexOf(<!DEBUG_INFO_SMARTCAST!>a<!>.toLowerCase())
}