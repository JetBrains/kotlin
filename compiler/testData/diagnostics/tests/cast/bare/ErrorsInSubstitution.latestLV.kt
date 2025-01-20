// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// LATEST_LV_DIFFERENCE

interface B<T>
interface G<T>: B<T>

fun f(p: B<<!UNRESOLVED_REFERENCE!>Foo<!>>): Any {
    val v = p as <!NO_TYPE_ARGUMENTS_ON_RHS!>G<!>
    return checkSubtype<G<*>>(v)
}