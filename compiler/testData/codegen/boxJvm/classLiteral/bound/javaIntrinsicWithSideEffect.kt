// TARGET_BACKEND: JVM

// WITH_STDLIB

fun box(): String {
    var x = 42
    val k = (x++)::class.java
    if (k != Int::class.java) return "Fail 1: $k"
    if (x != 43) return "Fail 2: $x (side effect should have taken place)"

    return "OK"
}
