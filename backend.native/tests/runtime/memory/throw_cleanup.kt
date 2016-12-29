fun main(args: Array<String>) {
    foo(false)
    try {
        foo(true)
    } catch (e: Error) {
        println("Ok")
    }
}

fun foo(b: Boolean): Any {
    var result = Any()
    if (b) {
        throw Error()
    }
    return result
}