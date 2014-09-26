fun box(): String {
    val args = array("Fail")
    (A::main)(args)
    return args[0]
}
