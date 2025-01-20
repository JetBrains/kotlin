// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// LATEST_LV_DIFFERENCE

class G<T>

fun foo(p: <!UNRESOLVED_REFERENCE!>P<!>) {
    val v = p as G?
    checkSubtype<G<*>>(v!!)
}
