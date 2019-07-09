fun box(): String = "OK"


class E(s: String) : Exception(s) {

}

fun testEmpty(ss: List<String>) {
    for (s in ss);
}

fun main(args: Array<String>) {
    if (box() == "OK") {
        System.out.println("Hello")
        println("Hello")
        throw E("Hello")
    }
}