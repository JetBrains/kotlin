// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

interface Callback {
    fun onSuccess()
}

public interface SendChannelX<in E> {
    public fun close(cause: Throwable? = null): Boolean
}

public interface ProducerScopeX<in E>   {
    public val channel: SendChannelX<E>
    fun foo(x: E)
}

public class FlowX<out T> {}

@OptIn(ExperimentalTypeInference::class)
public fun <T> callbackFlowX(@BuilderInference block: ProducerScopeX<T>.() -> Unit): FlowX<T> = FlowX()

fun foo(): FlowX<String> = callbackFlowX {
    object : Callback {
        override fun onSuccess() {
            channel.close()
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}