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
    accept(<!INVISIBLE_REFERENCE("fun of(vararg x: String): WithPrivateOf; private; 'WithPrivateOf.Companion'")!>[]<!>)
    accept(<!INVISIBLE_REFERENCE("fun of(vararg x: String): WithPrivateOf; private; 'WithPrivateOf.Companion'")!>["!"]<!>)

    val wpo: WithPrivateOf = <!INVISIBLE_REFERENCE("fun of(vararg x: String): WithPrivateOf; private; 'WithPrivateOf.Companion'")!>[]<!>

    val res = when {
        true -> WithPrivateOf()
        else -> <!INVISIBLE_REFERENCE("fun of(vararg x: String): WithPrivateOf; private; 'WithPrivateOf.Companion'")!>[]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, objectDeclaration,
operator, propertyDeclaration, stringLiteral, vararg, whenExpression */
