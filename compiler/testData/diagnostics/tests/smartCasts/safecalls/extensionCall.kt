fun String.bar(s: String) = s

fun foo(s: String?) {
    s?.bar(<!DEBUG_INFO_SMARTCAST!>s<!>)
    s?.get(<!DEBUG_INFO_SMARTCAST!>s<!>.length)
}