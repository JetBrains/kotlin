// FILE: A.kt
annotation class Annotation

private val ok = "OK"

internal inline fun internalInlineFun(): String {
    val obj = object {
        @Annotation
        val a = ok
    }

    return obj.a
}

// FILE: B.kt
fun box(): String = internalInlineFun()