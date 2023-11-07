// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun foo(x: Int?) {}
fun foo(y: String?) {}
fun foo(z: Boolean) {}

fun <T> baz(element: (T) -> Unit): T? = null

fun test1() {
    val a1: Int? = baz(::foo)
    val a2: String? = baz(::foo)
    val a3: Boolean? = baz<Boolean>(::foo)

    baz<Int>(::foo).checkType { _<Int?>() }
    baz<String>(::foo).checkType { _<String?>() }
    baz<Boolean>(::foo).checkType { _<Boolean?>() }

    val b1: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>baz(::<!NONE_APPLICABLE!>foo<!>)<!>
    val b2: String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>baz(::<!NONE_APPLICABLE!>foo<!>)<!>
    val b3: Boolean = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>baz(::<!NONE_APPLICABLE!>foo<!>)<!>
}
