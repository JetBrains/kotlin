fun box(): String = "OK"


class E(s: String) : Exception(s) {

}

fun main(args: Array<String>) {
    if (box() == "OK") {
        System.out.println("Hello")
        throw E("Hello")
    }
}