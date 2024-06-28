// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -REDUNDANT_PROJECTION

class FunctionHolder<out T : Any>(val f: (@UnsafeVariance T) -> Unit) {
    fun f2(v: @UnsafeVariance T) {}
}

fun caller(
    holder1: FunctionHolder<out Any>,
    holder2: FunctionHolder<*>,
    holder3: FunctionHolder<Any>,
    a: Any
) {
    holder1.f(a)
    holder1.f2(a)

    holder2.f(a)
    holder2.f2(a)

    holder3.f(a)
    holder3.f2(a)
}
