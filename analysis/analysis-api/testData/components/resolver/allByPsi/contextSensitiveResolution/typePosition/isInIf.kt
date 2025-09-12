// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class SealedClass {
    open class SealedInheritor1(val prop1: String = "1"): SealedClass()
    class SealedInheritor2(val prop2: Int = 2): SealedClass()

    sealed class SealedSealedInheritor1(val prop3: Boolean = true): SealedClass()

    class IndirectSealedInheritor: SealedInheritor1()

    inner class Inner(val innerProp: String = "inner prop"): SealedClass()

    class Nested1
}

fun testIsInIf(instance: SealedClass): String {
    if (instance is SealedInheritor1) {
        return instance.prop1
    }

    if (instance is IndirectSealedInheritor) {
        return instance.prop1
    }

    if (instance is Inner) {
        return instance.innerProp
    }

    if (instance !is SealedInheritor1 && instance !is SealedSealedInheritor1) {
        if (instance is SealedInheritor2) {
            return instance.prop2
        }
    }

    if (instance is SealedSealedInheritor1) {
        return instance.prop3
    }

    if (instance is Nested1) {
        return "nested1"
    }

    return ""
}
