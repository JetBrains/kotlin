fun main(args: Array<String>) {
    if (A.foo() != 42) throw Exception()
    if (A.bar != "OK") throw Exception()
}
