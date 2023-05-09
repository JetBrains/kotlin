// TARGET_BACKEND: JVM_IR
// ISSUE: KT-57876

// MODULE: lib
interface EnvironmentKeyProvider {
    interface NestedInterface : @EnvironmentKeyDescription Some

    interface Some

    @Target(AnnotationTarget.TYPE)
    annotation class EnvironmentKeyDescription
}

// MODULE: main(lib)
fun bar(arg: EnvironmentKeyProvider.NestedInterface) {}
fun box() = "OK"
