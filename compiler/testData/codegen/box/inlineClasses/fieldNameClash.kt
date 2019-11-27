// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM, JVM_IR, JS, JS_IR, NATIVE
// There is a bug in the type mapper which results in an incorrect asm type for Z
// when the names of the inline class backing field and an extension property clash.
// https://youtrack.jetbrains.com/issue/KT-31927

inline class Z(val s: String) {
    val Int.s: Int get() = 42
}

fun box(): String {
    if (Z("a").toString() == "Z(s=\"a\")")
        return "OK"
    return "Fail"
}
