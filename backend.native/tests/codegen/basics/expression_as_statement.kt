fun foo() {
    Any() as String
}

fun main(args: Array<String>) {
    try {
        foo()
    } catch (e: Throwable) {
        println("Ok")
        return
    }

    println("Fail")
}