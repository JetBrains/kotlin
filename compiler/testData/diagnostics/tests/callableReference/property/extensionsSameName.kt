// FIR_IDENTICAL
// SKIP_TXT
// DIAGNOSTICS: -UNUSED_PARAMETER

fun check1(p: kotlin.reflect.KProperty1<*, *>) {}
fun check2(p: kotlin.reflect.KProperty1<in String, *>) {}

val CharSequence.x: Any get() = this
val BooleanArray.x: Any get() = this

fun box() {
    check1(CharSequence::x) // error in NI, ok in OI
    check2(CharSequence::x)
}
