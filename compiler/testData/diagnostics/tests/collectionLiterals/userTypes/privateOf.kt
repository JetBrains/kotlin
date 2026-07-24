// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// WITH_STDLIB

class WithPrivateOf {
    companion object {
        private operator fun of(vararg x: String) = WithPrivateOf()

        fun test() {
            accept([])
            accept(["!"])

            val wpo: WithPrivateOf = []

            val res = when {
                true -> wpo
                else -> []
            }
        }
    }

    fun test() {
        accept([])
    }
}

fun accept(s: WithPrivateOf) = Unit

fun test() {
    accept(<!UNRESOLVED_COLLECTION_LITERAL("WithPrivateOf")!>[]<!>)
    accept(<!UNRESOLVED_COLLECTION_LITERAL("WithPrivateOf")!>["!"]<!>)

    val wpo: WithPrivateOf = <!UNRESOLVED_COLLECTION_LITERAL("WithPrivateOf")!>[]<!>

    val res = when {
        true -> WithPrivateOf()
        else -> <!CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, objectDeclaration,
operator, propertyDeclaration, stringLiteral, vararg, whenExpression */
