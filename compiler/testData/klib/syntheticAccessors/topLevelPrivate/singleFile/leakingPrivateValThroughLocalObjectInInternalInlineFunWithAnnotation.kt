annotation class Annotation

private val ok = "OK"

internal inline fun internalInlineFun(): String {
    val obj = object {
        @Annotation
        val a = ok
    }

    return obj.a
}

fun box(): String = internalInlineFun()