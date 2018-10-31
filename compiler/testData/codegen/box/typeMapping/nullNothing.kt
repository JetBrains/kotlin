fun box(): String {
    // kotlin.Nothing should not be loaded here
    return (if (null is Nothing?) "O" else "FAIL1") + (if (null !is Nothing) "K" else "FAIL2")
}