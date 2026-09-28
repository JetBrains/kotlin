// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// DUMP_CFG
import kotlin.reflect.KClass

fun test(x: String): KClass<*> {
    return x.let { it::class }
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, lambdaLiteral, outProjection, starProjection */
