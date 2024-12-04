class Builder {
    var version: String = ""

    fun execute() {
        println(version)
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)

val builder = build {
    version = "321"
}

bui<caret>lder.execute()
