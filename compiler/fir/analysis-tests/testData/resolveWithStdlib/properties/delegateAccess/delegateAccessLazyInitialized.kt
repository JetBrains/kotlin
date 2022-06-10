import kotlin.reflect.KProperty

val <T> T.delegate get() = this

val state by lazy { false }

fun box(): String {
    if (state#isInitialized()) {
        return "FAIL: we shouldn't know the state for now"
    }

    if (state#delegate.isInitialized()) {
        return "FAIL: we shouldn't know the state for now"
    }

    val observed = state

    if (!state#isInitialized()) {
        return "FAIL: we've observed the state already"
    }

    if (!state#delegate.isInitialized()) {
        return "FAIL: we've observed the state already"
    }

    return "OK"
}
