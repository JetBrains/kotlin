// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtBinaryExpression
open class Builder {
    var version: String = ""

    fun execute() {
        println(version)
    }

    class NestedBuilder : Builder()
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

val builder = build {
    version = "321"
}

builder.execute()
<expr>builder.version = ""</expr>
