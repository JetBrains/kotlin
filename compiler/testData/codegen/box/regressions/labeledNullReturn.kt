// WITH_RUNTIME

fun id(s: String) = s

val ok = listOf("OK", null).mapNotNull { f ->
    id(f ?: return@mapNotNull null)
}

fun box() = ok
