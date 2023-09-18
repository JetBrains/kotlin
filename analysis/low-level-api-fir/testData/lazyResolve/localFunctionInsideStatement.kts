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
        fun do<caret>o() {

        }
    }

    execute()
}

