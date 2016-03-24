import test.*

fun box(): String {
    var result = "fail"

    makeRunnable<String> { result = "OK" }.run()

    return result
}

