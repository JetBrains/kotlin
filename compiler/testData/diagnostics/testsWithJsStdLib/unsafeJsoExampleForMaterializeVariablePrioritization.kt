// RUN_PIPELINE_TILL: BACKEND
// DUMP_INFERENCE_LOGS: FIXATION

fun <W : Any> unsafeJso(): W = js("({})")

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
