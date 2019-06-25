fun foo(): String{
    val bar: String by lazy {
        "OK"
    }

    return bar
}

