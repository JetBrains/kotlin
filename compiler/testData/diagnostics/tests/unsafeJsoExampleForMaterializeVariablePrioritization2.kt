// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN

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
            { assign(unsafeJso(), it).apply(builder) },
        )
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, typeConstraint,
typeParameter, typeWithExtension, vararg */
