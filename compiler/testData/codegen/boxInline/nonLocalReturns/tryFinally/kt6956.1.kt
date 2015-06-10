import test.*

inline fun <T> bar(arg: String, action: () -> T) {
    try {
        action()
    } finally {
        arg.length()
    }
}

fun box(): String {
    foo() {
        bar("") {
            return "OK"
        }
    }

    return "fail"
}