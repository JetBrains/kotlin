// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-71533
class Foo

fun main() {
    println(Foo::class.qualifiedName)
}
