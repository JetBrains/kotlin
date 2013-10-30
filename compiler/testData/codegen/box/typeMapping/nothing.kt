fun box(): String {
    // jet.Nothing should not be loaded here
    val x = "" is Nothing
    return "OK"
}