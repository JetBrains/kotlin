// WITH_REFLECT

@JvmName("Fail")
fun OK() {}

fun box() = ::OK.name
