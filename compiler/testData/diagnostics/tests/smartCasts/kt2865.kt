operator fun <K, V> MutableMap<K, V>.set(<!UNUSED_PARAMETER!>k<!>: K, <!UNUSED_PARAMETER!>v<!>: V) {}

fun foo(a: MutableMap<String, String>, x: String?) {
    a[x!!] = <!DEBUG_INFO_SMARTCAST!>x<!>
    a[<!DEBUG_INFO_SMARTCAST!>x<!>] = x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
}

fun foo1(a: MutableMap<String, String>, x: String?) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>a[x]<!> = x!!
    a[x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>] = <!DEBUG_INFO_SMARTCAST!>x<!>
}