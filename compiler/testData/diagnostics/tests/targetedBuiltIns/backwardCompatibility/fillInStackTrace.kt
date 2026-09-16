// RUN_PIPELINE_TILL: CODEGEN
class ControlFlowException : Exception("") {
    fun fillInStackTrace() = this
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, override, stringLiteral, thisExpression */
