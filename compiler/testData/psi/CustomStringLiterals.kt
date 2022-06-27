fun test() {
    "string without prefix"
    string"with prefix"
    val a = 1
    val b = 3
    s"string with prefix and args: ${a}, $b"
    val c = a
    "'a' doesn't considered as string template prefix"
}