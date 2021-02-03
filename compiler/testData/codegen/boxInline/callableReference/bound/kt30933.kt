// IGNORE_BACKEND: JVM
// FILE: 1.kt

package test

class Path {
    val events: String = "OK"
}

inline fun doSomething(path: Path): String {
    val f = path::events
    return f()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return doSomething(Path())
}
