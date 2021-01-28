// IGNORE_BACKEND_FIR: JVM_IR
// FILE: A.kt

package a

inline class Message(val value: String)

class Box {
    internal fun result(msg: Message): String = msg.value
}

// FILE: B.kt

fun box(): String {
    return a.Box().result(a.Message("OK"))
}
