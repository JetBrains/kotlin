open class Builder {
    var version: String = ""

    fun execute() {
        println(version)
    }

    class NestedBuilder : Builder()
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)

<expr>build {
    version = "123"
    class A {
        fun doo() {

        }
    }

    execute()
}</expr>

val builder = build {
    version = "321"
}

builder.execute()
builder.version = ""
