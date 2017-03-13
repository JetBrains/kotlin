fun foo(): String {
    val s = try {
        "OK"
    } catch (e: Exception) {
        try {
            ""
        } catch (ee: Exception) {
            ""
        }
    }

    return s
}

fun box(): String {
    return foo()
}