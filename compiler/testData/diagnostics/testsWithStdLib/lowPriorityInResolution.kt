// RUN_PIPELINE_TILL: CODEGEN
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
@kotlin.internal.LowPriorityInOverloadResolution
fun foo(): Int = 1

fun foo(): String = ""

fun test() {
    val s = foo()
    s.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration, stringLiteral */
