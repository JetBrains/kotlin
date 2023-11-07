// ISSUE: KT-54400

class Buildee<T: Any> {
    lateinit var variable: T
}

fun <T: Any> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

fun box(): String {
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        variable = 42
    }
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        variable = ""
    }
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        variable = {}
    }
    return "OK"
}
