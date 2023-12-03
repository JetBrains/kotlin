// ISSUE: KT-63638
// !LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: expect.kt

import kotlin.jvm.JvmInline

class Session<T>(val value: T)

@JvmInline
value class SessionMutex<T> private constructor(
    private val currentSessionHolder: AtomicReference<Session<T>?>
) {
    constructor() : this(AtomicReference(null))

    val currentSession: T?
        get() = currentSessionHolder.get()?.value
}

expect class AtomicReference<V>(value: V) {
    fun get(): V
    fun set(value: V)
    fun getAndSet(value: V): V
    fun compareAndSet(expect: V, newValue: V): Boolean
}

// MODULE: main()()(common)
// FILE: actual.kt

internal actual typealias AtomicReference<V> = java.util.concurrent.atomic.AtomicReference<V>

fun box(): String {
    val mutex = SessionMutex<Int>()
    return "OK"
}
