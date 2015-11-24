import test.*

fun box(): String {
    var result = "fail"
    W("OK").safe {
        {
            result = this as String
        }()
    }

    return result
}