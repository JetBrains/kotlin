// IGNORE_BACKEND: JS
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt
var log = ""

val a1 = "a".also {
    log += "a1"
}
val b1 = a2.also {
    log += "b1"
}
val c1 = a3.also {
    log += "c1"
}

// FILE: B.kt
val a2 = a1.also {
    log += "a2"
}
val b2 = "b".also {
    log += "b2"
}

// FILE: C.kt
val a3 = b1.also {
    log += "a3"
}
val b3 = b2.also {
    log += "b3"
}
val c3 = "c".also {
    log += "c3"
}

// FILE: main.kt

fun box(): String = if (log == "a1a2b2b1a3b3c3c1") "OK" else "fail: $log"