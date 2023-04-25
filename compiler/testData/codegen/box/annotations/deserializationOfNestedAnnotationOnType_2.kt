// TARGET_BACKEND: JVM_IR
// ISSUE: KT-57876

// MODULE: lib
interface EnvironmentKeyProvider {
    @Target(AnnotationTarget.TYPE)
    annotation class EnvironmentKeyDescription

    fun getKnownKeys(arg: @EnvironmentKeyDescription String)
}

// MODULE: main(lib)
fun foo(arg: EnvironmentKeyProvider) {}
fun box() = "OK"
