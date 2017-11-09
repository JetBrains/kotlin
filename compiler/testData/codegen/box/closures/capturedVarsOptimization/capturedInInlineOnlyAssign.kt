// WITH_RUNTIME

fun box(): String {
    var x = ""
    run { x = "OK" }
    return x
}