// ISSUE: KT-84224

// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND

fun interface SuperSam { fun run() }
fun interface SubSam: SuperSam

fun foo(param: SubSam): Int = 42          // (1)
fun <T> foo(param: SubSam): String = "42" // (2)
fun foo(param: SuperSam): Double = 42.0   // (3)

fun main() {
    val sink: Int = foo {} // should resolve to (1)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, integerLiteral, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, samConversion, stringLiteral, typeParameter */
