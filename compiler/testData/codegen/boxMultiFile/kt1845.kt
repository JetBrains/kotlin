// FILE: 1.kt

public var v1: String = "V1"

fun box(): String {
    val s = "v1: $v1, v2: $v2"
    return "OK"
}

// FILE: 2.kt

public var v2: String = "V2"
