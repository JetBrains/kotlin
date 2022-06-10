import kotlin.reflect.KProperty

fun box(): String {
    val state by lazy { false }

    if (state#isInitialized()) {
        return "FAIL: we shouldn't know the state for now"
    }

    val observed = state

    if (!state#isInitialized()) {
        return "FAIL: we've observed the state already"
    }

    return "OK"
}
