// TARGET_BACKEND: JVM_IR
// Does not pass on other backends for various reasons, e.g.
// - FIR/WASM: IR declaration with signature "/x|{}x[0]" found in SymbolTable and not found in declaration storage
// - IR/JS: IrPropertySymbolImpl for /x|{}x[0] is already bound: PROPERTY name:x visibility:public modality:FINAL [val]
// - Old BE: Couldn't inline method call: with(A()) { ... }
// - See also: KT-57584, KT-58110
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-53718

class A

context(_: A)
val x: Int
    get() = 1

val x: Int
    get() = 2

context(a: A)
fun foo() = 3

fun foo() = 4

fun box(): String {
    if (x != 2) return "x = $x"
    if (foo() != 4) return "foo() = ${foo()}"

    // No syntax for explicit context arguments for properties
    // if (x != 1) return "context x = $x"
    if (foo(a = A()) != 3) return "context foo() = ${foo()}"

    return "OK"
}
