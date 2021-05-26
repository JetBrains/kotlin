// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Builder<T> {
    suspend fun add(t: T) {}
}

fun <S> build(g: suspend Builder<S>.() -> Unit): List<S> = TODO()
fun <S> wrongBuild(g: Builder<S>.() -> Unit): List<S> = TODO()

fun <S> Builder<S>.extensionAdd(s: S) {}

suspend fun <S> Builder<S>.safeExtensionAdd(s: S) {}

val member = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
    add(42)
}

val memberWithoutAnn = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>wrongBuild<!> {
    add(42)
}

val extension = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
    extensionAdd("foo")
}

val safeExtension = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
    safeExtensionAdd("foo")
}
