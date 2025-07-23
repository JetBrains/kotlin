// SKIP_SPLITTING_TO_TWO_MODULES: ANY
// MODULE: lib
// FILE: A.kt

package a

inline class Message(val value: String)

class Box {
    internal fun result(msg: Message): String = msg.value
}

// MODULE: main()(lib)
// FILE: B.kt

fun box(): String {
    return a.Box().result(a.Message("OK"))
}
