// !WITH_NEW_INFERENCE

fun <T : Any> nullable(): T? = null

val value = nullable<Int>() ?: nullable()