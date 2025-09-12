// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface SealedInterface {
    class NestedInheritor(prop: String): SealedInterface
}

sealed class SealedClass {
    open class SealedInheritor1(val prop1: String = "1"): SealedClass()
    class SealedInheritor2(val prop2: Int = 2): SealedClass()

    sealed class SealedSealedInheritor1(val prop3: Boolean = true): SealedClass()

    class IndirectSealedInheritor: SealedInheritor1()

    class Nested {
        class DeepSealedNestedInheritor: SealedClass()
        class DeepIndirectSealedNestedInheritor: SealedInheritor1()
    }

    inner class Inner(val innerProp: String = "inner prop"): SealedClass()

    class Nested1
    class Nested2
    sealed class SealedNested

    companion object Companion
}

fun testAs(instance: SealedClass) {
    val SealedInheritor1 = instance as SealedInheritor1
    val SealedInheritor2 = instance as SealedInheritor2
}

fun testTypeAnnotation(arg: SealedClass) {
    val left: SealedInheritor1 = arg
    val i: SealedClass = SealedInheritor1()
}

class SealedInheritor1Inheritor: SealedInheritor1()

class Generic<T: SealedInheritor1>

class Generic2<T>

fun testTypeArg() {
    val i = Generic2<SealedInheritor1>()
}

fun <T : SealedClass> constraintFun() {}

fun testConstraint() {
    constraintFun<SealedInheritor1>()
}

class SubSealed : SealedInterface, NestedInheritor
