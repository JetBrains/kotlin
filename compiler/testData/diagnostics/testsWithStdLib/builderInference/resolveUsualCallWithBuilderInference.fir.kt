// ALLOW_KOTLIN_PACKAGE
// !LANGUAGE: +UnrestrictedBuilderInference
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

val memberWithoutAnn = wrongBuild {
    add(42)
}

val extension = build {
    extensionAdd("foo")
}

val safeExtension = build {
    safeExtensionAdd("foo")
}
