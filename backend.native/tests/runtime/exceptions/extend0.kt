class C : Exception("OK")

fun main(args: Array<String>) {
    try {
        throw C()
    } catch (e: Throwable) {
        println(e.message!!)
    }
}