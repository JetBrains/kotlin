// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ExplicitBackingFields

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
        @Suppress("PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS")
        get() = field.asImmutable()
}

fun box(): String {
    val my = My()
    if (my.storage is MutableStorage) {
        return "MUTABLE"
    }
    return my.storage.s
}
