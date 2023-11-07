// WITH_STDLIB
// ISSUE: KT-41728

fun foo(a: String?) {
    if (a == null) {
        return
        <!UNREACHABLE_CODE!>"hi".length<!>
    }
    a.length
}
