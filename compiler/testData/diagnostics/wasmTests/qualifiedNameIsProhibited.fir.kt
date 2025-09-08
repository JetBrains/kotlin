// ISSUE: KT-71533
// WASM_DISABLE_FQNAME_IN_KCLASS
class Foo

fun main() {
    println(Foo::class.<!UNSUPPORTED_REFLECTION_API!>qualifiedName<!>)
}
