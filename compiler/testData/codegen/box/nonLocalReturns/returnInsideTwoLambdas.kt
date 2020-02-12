// !LANGUAGE: -NewInference
// IGNORE_BACKEND_FIR: JVM_IR
fun <T> outer(command: () -> T) : T = command()

inline fun <K> inner(action: () -> K): K = action()

fun test1(): String {
    outer {
        inner {
            return@outer
        }
    }

    return "O"
}

fun test2(): String {
    outer {
        return@outer
        inner {
        }
    }

    return "K"
}


fun box(): String {
    return test1() + test2()
}