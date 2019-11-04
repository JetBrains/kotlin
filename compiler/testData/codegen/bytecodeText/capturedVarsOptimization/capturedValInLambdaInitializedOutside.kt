fun box(): String {
    val x: String
    x = "OK"
    {
        val y = x
    }()
    return x
}

// 0 ObjectRef
