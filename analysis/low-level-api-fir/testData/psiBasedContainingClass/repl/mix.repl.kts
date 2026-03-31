package foo.bar

class Builder {
    var version: String = ""

    init {
        val x = 1
    }

    fun execute() {
        println(version)
    }

    class Nested {
        fun foo() {}
        fun boo() {}
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)
fun build2(action: Builder.() -> Unit): Builder = Builder().apply(action)

build {
    version = "123"
    class A {
        fun doo() {

        }
    }

    execute()
}

val builder = build {
    version = "321"
}

builder.version = ""
builder.execute()
