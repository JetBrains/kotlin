// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class Enum {
    ENUM_VALUE {
        override fun test() = ENUM_VALUE
    };

    abstract fun test(): Enum
}

fun box(): String {
    if (Enum.ENUM_VALUE.test() != Enum.ENUM_VALUE) return "fail"
    return "OK"
}
