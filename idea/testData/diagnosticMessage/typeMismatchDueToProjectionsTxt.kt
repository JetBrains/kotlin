// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS
// !MESSAGE_TYPE: TEXT

fun foo(x: MutableCollection<out CharSequence>, y: MutableCollection<CharSequence>) {
    x.addAll(y)
}
