// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// LATEST_LV_DIFFERENCE

interface Tr
interface G<T>

fun test(tr: Tr?) {
    val v = tr as G?
    checkSubtype<G<*>>(v!!)
}