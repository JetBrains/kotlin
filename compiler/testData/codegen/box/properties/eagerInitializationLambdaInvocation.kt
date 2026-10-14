// ISSUE: KT-52223
// IGNORE_BACKEND: JVM, NATIVE
// ^EagerInitialization is not supported on JVM, Native has different behaviour
// PROPERTY_LAZY_INITIALIZATION

// FILE: lib.kt
var initialized = false

// FILE: lib2.kt

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val eagerLambda = { }

val lazyProperty = run { initialized = true }

// FILE: main.kt

fun box(): String {
    eagerLambda()
    if (initialized) return "fail: eagerLambda() initialized lazyProperty of its own file"
    lazyProperty
    if (!initialized) return "fail: lazyProperty was never initialized"
    return "OK"
}
