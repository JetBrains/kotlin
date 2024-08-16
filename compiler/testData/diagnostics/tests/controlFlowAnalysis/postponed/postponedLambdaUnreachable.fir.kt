// ISSUE: KT-70584

fun test(): () -> Byte {
    throw Exception("")
    return B(A(fun (): Byte = 0, fun(): Byte = 1)).f.first
}

open class B(var f: A<() -> Byte>)

class A<G>(var first : () -> Byte, val secondary: G)
