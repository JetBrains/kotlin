// ISSUE: KT-71533
class Foo

fun main() {
    println(Foo::class.<!UNSUPPORTED_REFLECTION_API!>qualifiedName<!>)
}
