// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// ISSUE: KT-58252

// MODULE: lib-common
// FILE: common.kt

package foo

interface Base<T : Any> {
    val capacity: Int
}

expect abstract class Derived<T : Any>(capacity: Int) : Base<T> {
    final override val capacity: Int
}

internal val ByteArrayPool = object : Derived<ByteArray>(128) {}

// MODULE: lib()()(lib-common)
// FILE: platform.kt

package foo

actual abstract class Derived<T : Any>
actual constructor(actual final override val capacity: Int) : Base<T> {
    private val instances = arrayOfNulls<Any?>(capacity)
}

fun box(): String {
    return if (ByteArrayPool.capacity == 128) "OK" else "Error: ${ByteArrayPool.capacity}"
}
