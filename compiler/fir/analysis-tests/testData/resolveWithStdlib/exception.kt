fun box(): String = "OK"

fun main(args: Array<String>) {
    if (box() == "OK") {
        throw Exception("Hello")
    }
}