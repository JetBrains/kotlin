fun main(args: Array<String>) {
    val ok = K { "O" }
    if (ok != "OK") throw java.lang.AssertionError("Expected: OK, actual: $ok")
}