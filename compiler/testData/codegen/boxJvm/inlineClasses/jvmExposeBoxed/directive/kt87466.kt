// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

// FILE: 2.kt

package repro

internal fun make(v: V): C = C(v)

fun box(): String {
    make(V(1L))
    return "OK"
}

// FILE: 1.kt
package repro

@JvmInline
internal value class V(val value: Long)

internal class C(v: V, f: () -> Unit = {})
