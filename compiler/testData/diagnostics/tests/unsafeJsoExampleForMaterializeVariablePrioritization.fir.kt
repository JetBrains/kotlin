// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS: FIXATION

fun <W : Any> unsafeJso(): W = TODO() // = js("({})")

interface State

fun <T : Any, R : T> assign(dest: R, vararg src: T?): R = TODO()

interface ReactComponentWrapper<S : State> {
    fun setState(transformState: (S) -> S)

    fun setState2(
        wrapper: ReactComponentWrapper<S>,
        builder: S.() -> Unit,
    ) {
        wrapper.setState(
            { <!RETURN_TYPE_MISMATCH!>assign(unsafeJso(), it).apply(<!ARGUMENT_TYPE_MISMATCH!>builder<!>)<!> },
        )
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, typeConstraint,
typeParameter, typeWithExtension, vararg */
