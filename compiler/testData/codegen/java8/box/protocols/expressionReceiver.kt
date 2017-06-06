// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
}

class Impl {
    companion object {
        var tmp = ""
    }

    init {
        tmp = "OK$tmp"
    }

    fun foo(): String {
        return tmp
    }
}

fun box(): String = Impl().foo()
