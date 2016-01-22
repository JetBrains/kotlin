// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: MEMBER_PROJECTED_OUT

fun foo(x: MutableCollection<out CharSequence>) {
    x.add("")
}
