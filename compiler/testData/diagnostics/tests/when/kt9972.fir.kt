// !WITH_NEW_INFERENCE
fun test1(): Int {
    val x: String = if (true) {
        when {
            true -> Any()
            else -> null
        }
    } else ""
    return x.hashCode()
}

fun test2(): Int {
    val x: String = when {
                        true -> Any()
                        else -> null
                    } ?: return 0
    return x.hashCode()
}