// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

interface Storage {
    val s: String
}

class ImmutableStorage(override val s: String) : Storage
class MutableStorage(override var s: String) : Storage {
    fun asImmutable() = ImmutableStorage(s)
}

class My {
    val storage: Storage
        field = MutableStorage("OK")
        get() = field.asImmutable()
}

fun box(): String {
    val my = My()
    if (my.storage is MutableStorage) {
        return "MUTABLE"
    }
    return my.storage.s
}
