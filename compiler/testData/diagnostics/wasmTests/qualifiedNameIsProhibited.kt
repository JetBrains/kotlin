// ISSUE: KT-71533
class Foo

fun main() {
    println(Foo::class.<!UNSUPPORTED!>qualifiedName<!>)
}
