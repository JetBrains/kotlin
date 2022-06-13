fun box(): String {
    val x: String
    x = "OK"
    {
        val y = x
    }.let { it() }
    return x
}

// 0 ObjectRef
