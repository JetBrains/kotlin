// ISSUE: KT-71533
// FIR_IDENTICAL
// WASM_ALLOW_FQNAME_IN_KCLASS
class Foo

fun main() {
    println(Foo::class.qualifiedName)
}
