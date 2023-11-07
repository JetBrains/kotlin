// ISSUE: KT-55057
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

class Buildee<PTV>

fun <PTV> Buildee<PTV>.yieldWithoutAnnotation(value: PTV) {}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference // note: that annotation was never intended to be used on functions without lambdas
fun <PTV> Buildee<PTV>.yieldWithAnnotation(t: PTV) {}

fun <PTV> build(block: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(block)
}

class TargetType

fun <T> make(): Buildee<T> = Buildee()

fun box(): String {
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        yieldWithoutAnnotation(make<TargetType>())
        yieldWithoutAnnotation(make()) // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    }
    // K1&K2: OK — TypeVariable(PTV) of build is inferred into Buildee<Buildee<TargetType>>
    build {
        yieldWithAnnotation(make<TargetType>())
        yieldWithAnnotation(make()) // K1&K2: OK — TypeVariable(T) of make is inferred into Buildee<TargetType>
    }
    return "OK"
}
