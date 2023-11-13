// TARGET_BACKEND: JVM_IR
// Does not pass on other backends for various reasons, e.g.
// - FIR/WASM: IR declaration with signature "/x|{}x[0]" found in SymbolTable and not found in declaration storage
// - IR/JS: IrPropertyPublicSymbolImpl for /x|{}x[0] is already bound: PROPERTY name:x visibility:public modality:FINAL [val]
// - Old BE: Couldn't inline method call: with(A()) { ... }
// - See also: KT-57584, KT-58110
// LANGUAGE: +ContextReceivers
// ISSUE: KT-53718

class A

context(A)
val x: Int
    get() = 1

val x: Int
    get() = 2

context(A)
fun foo() = 3

fun foo() = 4

fun box(): String {
    if (x != 2) return "x = $x"
    if (foo() != 4) return "foo() = ${foo()}"

    with(A()) {
        if (x != 1) return "context x = $x"
        if (foo() != 3) return "context foo() = ${foo()}"
    }

    return "OK"
}
