// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr?) {
    val v = tr as G
    checkSubtype<G<*>>(v)
}
