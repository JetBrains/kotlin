suspend fun dummy() {}

val c: suspend () -> Unit = {}

fun builder(c: suspend () -> Unit) {}

val d = suspend {}

suspend fun check() {
    dummy()
    c()
    builder {}
}