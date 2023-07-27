class Builder {
    var version: String = ""

    fun execute() {
        println(version)
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)

build {
    version = "123"
    class A {
        fun doo() {

        }
    }

    execute()
}

val builder = <expr>build</expr> {
    version = "321"
}

builder.execute()
