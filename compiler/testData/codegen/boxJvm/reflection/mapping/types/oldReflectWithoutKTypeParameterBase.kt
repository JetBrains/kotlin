// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// OPT_IN: kotlin.ExperimentalStdlibApi

// This test emulates behavior of pre-2.3.20 kotlin-reflect against kotlin-stdlib 2.3.20+.
// Prior to 2.3.20, KTypeParameterImpl did not inherit from KTypeParameterBase (which has appeared in kotlin-stdlib 2.3.20),
// and kotlin-stdlib implementation of javaType created a fake TypeVariable object instead of finding it in the type parameter's container.
// In this test, we're creating a custom KTypeParameter instance that is supposed to behave like KTypeParameterImpl did before 2.3.20, and
// checking that kotlin-stdlib implementation of javaType does not throw on it at least on operations that somehow worked when both
// kotlin-reflect and kotlin-stdlib were <2.3.20. However, it should still throw on access to getGenericDeclaration, which was only
// supported in 2.3.20 (KT-39661).

import java.lang.reflect.TypeVariable
import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private object MyTypeParameter : KTypeParameter {
    override val name: String get() = "A"
    override val upperBounds: List<KType> get() = emptyList()
    override val variance: KVariance get() = KVariance.INVARIANT
    override val isReified: Boolean get() = false
}

private object MyType : KType {
    override val classifier: KClassifier get() = MyTypeParameter
    override val arguments: List<KTypeProjection> get() = emptyList()
    override val isMarkedNullable: Boolean get() = false
    override val annotations: List<Annotation> get() = emptyList()
}

fun box(): String {
    val fakeTypeVariable = MyType.javaType as TypeVariable<*>
    assertEquals("A", fakeTypeVariable.name)
    assertEquals(emptyList(), fakeTypeVariable.bounds.toList())

    assertFailsWith(UnsupportedOperationException::class) { fakeTypeVariable.getGenericDeclaration() }

    return "OK"
}
