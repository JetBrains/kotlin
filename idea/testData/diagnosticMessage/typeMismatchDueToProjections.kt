// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS

fun foo(x: MutableCollection<out CharSequence>, y: MutableCollection<CharSequence>) {
    x.addAll(y)
}
