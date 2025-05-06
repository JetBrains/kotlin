// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
class Foo<K>

fun <K> buildFoo(builderAction: Foo<K>.() -> Unit): Foo<K> = Foo()

fun <K> Foo<K>.bar(x: Int = 1) {}

fun main() {
    val x = <!CANNOT_INFER_PARAMETER_TYPE!>buildFoo<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>()
    }
}
