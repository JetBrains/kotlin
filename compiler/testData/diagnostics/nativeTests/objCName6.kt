// FIR_IDENTICAL
// FILE: kotlin.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

fun interface AutoCloseable {
    @ObjCName("close") fun close()
}

interface BaseStream<T, S : BaseStream<T, S>> : AutoCloseable {
    override fun close()
}

interface Stream<T> : BaseStream<T, Stream<T>> {}

open class TerminatableStream<T : TerminatableStream<T>> {
    @ObjCName("close") open fun close() {}
}

class StreamImpl<T> : TerminatableStream<StreamImpl<T>>, Stream<T> {
    constructor() : super() {}
}