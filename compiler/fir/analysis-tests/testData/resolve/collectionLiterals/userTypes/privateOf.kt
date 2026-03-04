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
    accept(<!ARGUMENT_TYPE_MISMATCH("List<uninferred T (of fun <T> listOf)>; WithPrivateOf"), CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!>)
    accept(<!ARGUMENT_TYPE_MISMATCH("List<String>; WithPrivateOf")!>["!"]<!>)

    val wpo: WithPrivateOf <!INITIALIZER_TYPE_MISMATCH("WithPrivateOf; List<uninferred ??? (Unknown type for type parameter T)>")!>=<!> <!CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!>

    val res = when {
        true -> WithPrivateOf()
        else -> <!CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, objectDeclaration,
operator, propertyDeclaration, stringLiteral, vararg, whenExpression */
