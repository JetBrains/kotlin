// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-55057
// CHECK_TYPE_WITH_EXACT
// WITH_STDLIB
// API_VERSION: 1.9

import kotlin.experimental.ExperimentalTypeInference

fun <ETV> Buildee<ETV>.yieldWithoutAnnotation(value: ETV) {}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference // note: this annotation was never intended to be used on functions without lambdas
fun <ETV> Buildee<ETV>.yieldWithAnnotation(t: ETV) {}

fun test() {
    val buildeeA = build {
        yieldWithoutAnnotation(materializeBuildee<TargetType>())
        yieldWithoutAnnotation(materializeBuildee())
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Buildee<TargetType>>>(buildeeA)

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

/* GENERATED_FIR_TAGS: classDeclaration, classReference, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter, typeWithExtension */
