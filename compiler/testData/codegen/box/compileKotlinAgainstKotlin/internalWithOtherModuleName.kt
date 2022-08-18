// MODULE: lib
// FILE: A.kt

package a

class Box(val value: String) {
    internal fun result(): String = value
}

// MODULE: main()(lib)
// FILE: B.kt

fun box(): String {
    return a.Box("OK").result()
}
