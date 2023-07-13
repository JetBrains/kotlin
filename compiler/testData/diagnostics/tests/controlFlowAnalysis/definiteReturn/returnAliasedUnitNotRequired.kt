// FIR_IDENTICAL
// ISSUE: KT-60299

private typealias T = Unit

internal fun x(): T {
    val something = "OK"
    something.hashCode()
}
