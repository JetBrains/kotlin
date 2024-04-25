// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// Issue: KT-40045

interface AAA

fun <K : AAA, R : K> assign(dest: R, vararg src: K?): R = null as R

class DIV(val tabIndex: String): AAA

fun <T: AAA> jsObject(builder: T.() -> Unit): T = null as T

fun foo(x: DIV) {
    assign(x, jsObject { tabIndex }) // tabIndex is resolved in OI and unresolved in NI because T is inferred to Any instead of DIV
}
