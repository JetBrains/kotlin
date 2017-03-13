fun box(): String {
    // kotlin.Nothing should not be loaded here
    val x = "" is Nothing
    return "OK"
}