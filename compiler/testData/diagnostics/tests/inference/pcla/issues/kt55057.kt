// ISSUE: KT-55057
// CHECK_TYPE_WITH_EXACT
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

fun <ETV> Buildee<ETV>.yieldWithoutAnnotation(value: ETV) {}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference // note: this annotation was never intended to be used on functions without lambdas
fun <ETV> Buildee<ETV>.yieldWithAnnotation(t: ETV) {}

fun test() {
    val buildeeA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        yieldWithoutAnnotation(materializeBuildee<TargetType>())
        yieldWithoutAnnotation(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materializeBuildee<!>())
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Buildee<TargetType>>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>buildeeA<!>)

    val buildeeB = build {
        yieldWithAnnotation(materializeBuildee<TargetType>())
        yieldWithAnnotation(materializeBuildee())
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Buildee<TargetType>>>(buildeeB)
}




fun <T> materializeBuildee(): Buildee<T> = Buildee()

class TargetType

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
