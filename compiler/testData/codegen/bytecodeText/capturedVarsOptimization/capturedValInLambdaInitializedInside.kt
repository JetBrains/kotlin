// WITH_STDLIB

fun box(): String {
    val x: String
    run {
        x = "OK"
        val y = x
    }
    return x
}

// 0 ObjectRef
