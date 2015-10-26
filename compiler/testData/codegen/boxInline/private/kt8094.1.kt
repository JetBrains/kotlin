import test.*

fun box(): String {
    var r = "fail"
    X.g { r = "OK" }

    return r;
}