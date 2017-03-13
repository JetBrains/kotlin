fun box(): String {
    val f = "KOTLIN"::get
    return "${f(1)}${f(0)}"
}
