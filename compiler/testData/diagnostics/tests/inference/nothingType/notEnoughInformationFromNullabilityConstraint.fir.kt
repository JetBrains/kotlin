// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE

fun <M> make(): M? = null
fun <I> id(arg: I): I = arg

val v = id(
    make()
)
