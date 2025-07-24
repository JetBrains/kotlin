// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper(val id: Int)

class DMap(private val map: Map<Wrapper, String>) :
        Map<Wrapper, String> by map

fun box(): String {
    val dmap = DMap(mutableMapOf(Wrapper(42) to "OK"))
    return dmap[Wrapper(42)] ?: "Fail"
}
