// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Z(val x: Int)
inline class NZ1(val nz: Z?)
inline class NZ2(val nz: NZ1)

fun box(): String {
    if (NZ2(NZ1(null)) != NZ2(NZ1(null))) throw AssertionError()
    if (NZ2(NZ1(Z(1))) != NZ2(NZ1(Z(1)))) throw AssertionError()
    if (NZ2(NZ1(null)) == NZ2(NZ1(Z(1)))) throw AssertionError()
    if (NZ2(NZ1(Z(1))) == NZ2(NZ1(null))) throw AssertionError()

    return "OK"
}
