class Builder {
    var version: String = ""

    fun execute() {
        println(version)
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)

build {
    <expr>version</expr> = "123"
    class A {
        fun doo() {

        }
    }

    execute()
}

val builder = build {
    version = "321"
}

builder.execute()
