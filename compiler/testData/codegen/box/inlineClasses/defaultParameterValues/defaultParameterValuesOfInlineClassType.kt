// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Z(val z: Int)

fun test(z: Z = Z(42)) = z.z

fun box(): String {
    if (test() != 42) throw AssertionError()
    if (test(Z(123)) != 123) throw AssertionError()

    return "OK"
}