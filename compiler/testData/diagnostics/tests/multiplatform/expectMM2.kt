// FIR_IDENTICAL

// MODULE: m1-common
// FILE: common.kt

interface SendChannel<in E> {
    suspend fun send(element: E)
}

interface Channel<E> : SendChannel<E>

// MODULE: m2-jvm()()(m1-common)
// FILE: int2.kt

class ChannelCoroutine<E>(
    protected val _channel: Channel<E>,
) : Channel<E> by _channel

// MODULE: m3-jvm()()(m1-common, m2-jvm)
// FILE: jvm.kt

suspend fun <E> main(channelCoroutine: ChannelCoroutine<E>, e: E) {
    channelCoroutine.send(e)
}
