// RESOLVE_SCRIPT

class Builder {
    var version: String = ""
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)

val builder = build {
    version = "321"
}

@Suppress("abc") @Deprecated("it is deprecated")
builder.execute()
