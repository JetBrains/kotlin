// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83449
// LANGUAGE: +CacheLocalVariableScopes
// FIR_DUMP

fun test() {
    var state: State = State.Default()
    captureVariable {
        state = State.Custom()
        state.id.length
    }
    state.id.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    state.index
    state = State.Default()
}


sealed interface State {
    val id: CharSequence
    val index: Int

    class Default(
        override val id: CharSequence = "default",
        override val index: Int = 0,
    ): State

    class Custom(
        override val id: String = "custom",
        override val index: Int = 0,
    ): State
}

fun captureVariable(block: () -> Unit) {}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, lambdaLiteral, localProperty, nestedClass, override, primaryConstructor, propertyDeclaration,
sealed, smartcast, stringLiteral */
