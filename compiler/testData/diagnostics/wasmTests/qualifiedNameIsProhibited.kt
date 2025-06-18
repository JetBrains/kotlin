// ISSUE: KT-71533
// FIR_IDENTICAL
// IGNORE_BACKEND_K2: WASM
class Foo

fun main() {
    println(Foo::class.<!UNSUPPORTED!>qualifiedName<!>)
}
