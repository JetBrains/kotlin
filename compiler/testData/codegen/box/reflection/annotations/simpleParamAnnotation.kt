// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

@Retention(AnnotationRetention.RUNTIME)
annotation class Simple(val value: String)

fun test(@Simple("OK") x: Int) {}

fun box(): String {
    return (::test.parameters.single().annotations.single() as Simple).value
}
