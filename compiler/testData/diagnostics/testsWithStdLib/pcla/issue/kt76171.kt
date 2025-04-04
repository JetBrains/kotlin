// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76171
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.reflect.KProperty


interface DialogScope<R> {
    var expectedValue: R
}

class DialogState {

    fun <R> dialog(block: (Continuation<R>) -> Unit) = Unit

    fun <R> awaitResult(
        initial: R,
    ) = dialog { cont ->
        val state = rememberA { fakeMutableStateOf(initial) }
        rememberA {
            object : DialogScope<R> {
                override var expectedValue by state
            }
        }
        cont.resume(state.value)
    }
}

fun <T> rememberA(
    calculation: () -> T
): T {
    TODO()
}

class FakeMutableState<T>(var value: T) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}

fun <T> fakeMutableStateOf(value: T): FakeMutableState<T> = FakeMutableState(value)