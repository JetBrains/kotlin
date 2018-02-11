// !WITH_NEW_INFERENCE
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

    val b1: Int = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>baz(::<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>)<!>
    val b2: String = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>baz(::<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>)<!>
    val b3: Boolean = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>baz(::<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>)<!>
}