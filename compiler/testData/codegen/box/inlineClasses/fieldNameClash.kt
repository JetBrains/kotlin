// IGNORE_BACKEND: JVM, JVM_IR
// There is a bug in the type mapper which results in an incorrect asm type for Z
// when the names of the inline class backing field and an extension property clash.

inline class Z(val s: String) {
    val Int.s: Int get() = 42
}

fun box(): String {
    if (Z("a").toString() == "Z(s=\"a\")")
        return "OK"
    return "Fail"
}
