// TARGET_BACKEND: JVM_IR
// ISSUE: KT-57876

// MODULE: lib
// FILE: lib.kt

interface EnvironmentKeyProvider {
    @Target(AnnotationTarget.TYPE)
    annotation class EnvironmentKeyDescription

    fun getKnownKeys(arg: @EnvironmentKeyDescription String)
}

// MODULE: main(lib)
// FILE: main.kt

fun foo(arg: EnvironmentKeyProvider) {}
fun box() = "OK"
