// IGNORE_BACKEND_K2_MULTI_MODULE: ANY
// ^^^ Cannot split to two modules due to cyclic import
// FILE: 1.kt

var result = "Fail"
val unused by c

fun box(): String = result

// FILE: 2.kt

class C {
    init {
        result = "OK"
    }
}

operator fun C.getValue(x: Any?, y: Any?): String = throw IllegalStateException()

val c = C()
