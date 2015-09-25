fun box(): String {
    val args = arrayOf("Fail")
    (A::main)(args)
    return args[0]
}
