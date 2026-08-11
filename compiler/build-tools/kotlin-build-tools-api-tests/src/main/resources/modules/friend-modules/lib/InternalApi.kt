package friends

internal fun internalGreeting(): String = "hello from the internal API"

fun publicGreeting(): String = internalGreeting()
