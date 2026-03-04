// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

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
    <!INVISIBLE_REFERENCE("fun accept(s: WithPrivateOf): Unit; public; file")!>accept<!>(<!INAPPLICABLE_CANDIDATE("fun of(vararg x: String): WithPrivateOf")!>[]<!>)
    <!INVISIBLE_REFERENCE("fun accept(s: WithPrivateOf): Unit; public; file")!>accept<!>(<!INAPPLICABLE_CANDIDATE("fun of(vararg x: String): WithPrivateOf")!>["!"]<!>)

    val wpo: WithPrivateOf = <!INAPPLICABLE_CANDIDATE("fun of(vararg x: String): WithPrivateOf")!>[]<!>

    val res = when {
        true -> WithPrivateOf()
        else -> <!INAPPLICABLE_CANDIDATE("fun of(vararg x: String): WithPrivateOf")!>[]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, objectDeclaration,
operator, propertyDeclaration, stringLiteral, vararg, whenExpression */
