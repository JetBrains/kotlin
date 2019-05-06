// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: MEMBER_PROJECTED_OUT
// !LANGUAGE: -NewInference

fun foo(x: MutableCollection<out CharSequence>) {
    x.add("")
}
