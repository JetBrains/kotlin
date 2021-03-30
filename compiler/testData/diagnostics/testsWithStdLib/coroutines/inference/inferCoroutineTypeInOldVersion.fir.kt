// !LANGUAGE: -ExperimentalBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

class Builder<T> {
    suspend fun add(t: T) {}
}

fun <S> build(g: suspend Builder<S>.() -> Unit): List<S> = TODO()
fun <S> wrongBuild(g: Builder<S>.() -> Unit): List<S> = TODO()

fun <S> Builder<S>.extensionAdd(s: S) {}

suspend fun <S> Builder<S>.safeExtensionAdd(s: S) {}

val member = build {
    add(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
}

val memberWithoutAnn = wrongBuild {
    add(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
}

val extension = build {
    extensionAdd(<!ARGUMENT_TYPE_MISMATCH!>"foo"<!>)
}

val safeExtension = build {
    safeExtensionAdd(<!ARGUMENT_TYPE_MISMATCH!>"foo"<!>)
}
