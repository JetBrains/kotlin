// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-40396

val <C> C.foo get() = Foo<C>()

class Foo<K> {
    operator fun <T> invoke(body: () -> Unit) {}
}

class Bar {
    val bar = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {}
    val baz = <!TYPE_ARGUMENTS_NOT_ALLOWED!>foo<!><Int> {}
}
