// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// LATEST_LV_DIFFERENCE

interface B<T>
interface G<T>: B<T>

fun f(p: B<<!UNRESOLVED_REFERENCE!>Foo<!>>): Any {
    val v = p <!UNCHECKED_CAST!>as <!DEPRECATED_BARE_DOWNCAST!>G<!><!>
    return checkSubtype<G<*>>(v)
}