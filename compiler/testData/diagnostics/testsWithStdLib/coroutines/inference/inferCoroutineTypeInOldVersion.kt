// !LANGUAGE: -ExperimentalBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Builder<T> {
    suspend fun add(t: T) {}
}

fun <S> build(g: suspend Builder<S>.() -> Unit): List<S> = TODO()
fun <S> wrongBuild(g: Builder<S>.() -> Unit): List<S> = TODO()

fun <S> Builder<S>.extensionAdd(s: S) {}

suspend fun <S> Builder<S>.safeExtensionAdd(s: S) {}

val member = build {
    add(42)
}

val memberWithoutAnn = <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>wrongBuild<!> {
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>add<!>(42)
}

val extension = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
    <!NI;NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE!>extensionAdd("foo")<!>
}

val safeExtension = build {
    safeExtensionAdd("foo")
}
