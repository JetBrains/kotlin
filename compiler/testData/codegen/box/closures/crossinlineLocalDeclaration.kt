// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

interface Wrapper { fun runBlock() }

inline fun crossInlineBuildWrapper(crossinline block: () -> Unit) = object : Wrapper {
    override fun runBlock() {
        block()
    }
}

class Container {
    val wrapper = crossInlineBuildWrapper {
        object { }
    }
}

fun box(): String {
    Container().wrapper.runBlock()
    return "OK"
}
