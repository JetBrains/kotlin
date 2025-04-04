// ISSUE: KT-76171
// IGNORE_BACKEND_K2: ANY
// WITH_STDLIB

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KProperty

interface DialogScope<R> {
    var expectedValue: R
}

fun <T> rememberA(
    calculation: () -> T
): T = calculation()

class FakeMutableState<T>(var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}

fun <T> fakeMutableStateOf(value: T): FakeMutableState<T> = FakeMutableState(value)

class DialogState {

    fun <R> dialog(block: (Continuation<R>) -> Unit): R {
        var result: Any? = null
        block(object : Continuation<R> {
            override fun resumeWith(p: Result<R>) {
                result = p.getOrNull()
            }

            override val context: CoroutineContext
                get() = TODO("Not yet implemented")
        })

        return result as R
    }

    fun <R> awaitResult(
        initial: R,
    ) = dialog { cont ->
        val state = rememberA { fakeMutableStateOf(initial) }
        rememberA {
            object : DialogScope<R> {
                override var expectedValue by state
            }
        }.expectedValue = "OK" as R
        cont.resume(state.value)
    }
}

fun box(): String {
    return DialogState().awaitResult("fail")
}