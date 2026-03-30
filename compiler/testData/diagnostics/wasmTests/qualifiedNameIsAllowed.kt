// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71533
class Foo

fun main() {
    println(Foo::class.qualifiedName)
}
