// !RENDER_DIAGNOSTICS_FULL_TEXT
// !LANGUAGE: -ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
class Foo<K>

fun <K> buildFoo(builderAction: Foo<K>.() -> Unit): Foo<K> = Foo()

fun <K> Foo<K>.bar(x: Int = 1) {}

fun main() {
    val x = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildFoo<!> {
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()
    }
}