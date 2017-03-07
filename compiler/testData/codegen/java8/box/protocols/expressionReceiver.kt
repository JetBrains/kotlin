// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
}

class Impl() {
    companion object {
        var tmp = ""
    }

    fun foo(): String {
        tmp = "OK$tmp"
        return "OK$tmp"
    }
}

fun box(): String = Impl().foo()
