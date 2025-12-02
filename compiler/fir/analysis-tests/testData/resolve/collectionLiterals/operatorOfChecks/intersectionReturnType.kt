// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

interface A {
    companion object {
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>of<!>(vararg str: String) = when {
            true -> C
            else -> D
        }
    }
}

interface B

object C: A, B
object D: A, B

/* GENERATED_FIR_TAGS: companionObject, functionDeclaration, interfaceDeclaration, objectDeclaration, operator, vararg,
whenExpression */
