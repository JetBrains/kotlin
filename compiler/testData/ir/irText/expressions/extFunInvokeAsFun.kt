// !DUMP_DEPENDENCIES

fun with1(receiver: Any?, block: Any?.() -> Unit) = block(receiver)
fun with2(receiver: Any?, block: Any?.() -> Unit) = receiver.block()