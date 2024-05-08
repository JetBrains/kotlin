// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +ImprovedCapturedTypeApproximationInInference
fun <R> sequenceOf(elements: Array<R>) {}

fun test(overriddenDescriptors: MutableCollection<out CharSequence>) {
    sequenceOf(overriddenDescriptors.toTypedArray())
}