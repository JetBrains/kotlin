// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: annotation.kt

package kotlin

annotation class BuilderInference

// FILE: test.kt

class Builder<T> {
    fun add(t: T) {}
}

fun <S> build(@BuilderInference g: Builder<S>.() -> Unit): List<S> = TODO()
fun <S> wrongBuild(g: Builder<S>.() -> Unit): List<S> = TODO()

fun <S> Builder<S>.extensionAdd(s: S) {}

@BuilderInference
fun <S> Builder<S>.safeExtensionAdd(s: S) {}

val member = build {
    add(42)
}

val memberWithoutAnn = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>wrongBuild<!> {
    add(42)
}

val extension = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
    extensionAdd("foo")
}

val safeExtension = build {
    safeExtensionAdd("foo")
}
