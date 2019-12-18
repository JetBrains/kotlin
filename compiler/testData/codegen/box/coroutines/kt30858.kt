// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES

class MyReceiveChannel<out E>
interface MyProducerScope<in E>
interface MyCoroutineScope

suspend inline fun <E> MyReceiveChannel<E>.myConsumeEach(action: (E) -> Unit) {}

suspend fun myDelay(timeMillis: Long) {}

fun myLaunch(
    block: suspend MyCoroutineScope.() -> Unit
) {}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
public fun <E> myProduce(@BuilderInference block: suspend MyProducerScope<E>.() -> Unit) {}

fun <T> MyReceiveChannel<T>.debounce(period: Long) {
    myProduce<Any> {
        myConsumeEach {
            myLaunch {
                myDelay(period)
            }
        }
    }
}

fun box(): String {
    val m = MyReceiveChannel<String>()
    m.debounce(42)

    return "OK"
}