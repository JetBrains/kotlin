fun box(): String? {
    var s: String?
    s = "FAIL for function literal"
    JavaClass.samAdapter { s = "OK"; null }
    if (s != "OK") return s

    s = "FAIL for wrapper"
    val function: (String?) -> Thread? = { s = "OK"; null }
    JavaClass.samAdapter(function)
    if (s != "OK") return s

    return "OK"
}
