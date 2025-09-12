// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-75977

sealed class SealedClass {
    open class SealedInheritor(val prop: String = "1"): SealedClass()
    class IndirectSealedInheritor: SealedInheritor()
}

fun testIsInWhenGuardSealedClass(instance: SealedClass): String {
    return when (instance) {
        is SealedInheritor if instance is IndirectSealedInheritor -> instance.prop
        else -> "100"
    }
}

open class OpenClass {
    open class SealedInheritorOfOpenClass(val prop: String = "1"): OpenClass()
    class IndirectSealedInheritorOfOpenClass: SealedInheritorOfOpenClass()
}

fun testIsInWhenGuardOpenClass(instance: OpenClass): String {
    return when (instance) {
        is SealedInheritorOfOpenClass if instance is IndirectSealedInheritorOfOpenClass -> instance.prop
        else -> "100"
    }
}
