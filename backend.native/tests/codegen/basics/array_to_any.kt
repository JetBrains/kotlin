fun main(args: Array<String>) {
    foo().hashCode()
}

fun foo(): Any {
    return Array<Any?>(0, { i -> null })
}