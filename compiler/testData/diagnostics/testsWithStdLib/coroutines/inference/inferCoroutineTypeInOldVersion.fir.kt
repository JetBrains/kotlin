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
    <!INAPPLICABLE_CANDIDATE!>add<!>(42)
}

val memberWithoutAnn = wrongBuild {
    <!INAPPLICABLE_CANDIDATE!>add<!>(42)
}

val extension = build {
    <!INAPPLICABLE_CANDIDATE!>extensionAdd<!>("foo")
}

val safeExtension = build {
    <!INAPPLICABLE_CANDIDATE!>safeExtensionAdd<!>("foo")
}
