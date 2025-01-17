// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class Ann<T>

class A
class B
private object FakeA {
    class A
}

@Ann<A>
expect fun sameTypeParam()

@Ann<B>
expect fun differentTypeParam()

@Ann<FakeA.A>
expect fun differentWithSameName()

@Ann<A?>
expect fun nonNullvsNull()

@Ann<Ann<in A>>
expect fun differentVariance()

@Ann<Ann<in A>>
expect fun varianceVsNoVariance()

@Ann<Ann<in A>>
expect fun sameVariance()

@Ann<Ann<*>>
expect fun startProjection()

annotation class ComplexNested<T>(
    vararg val anns: ComplexNested<*>,
)

@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<A>(),
)
expect fun complexSame()

@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<B>(),
)
expect fun complexDiffer()

annotation class NestedWithSameTypeArgument<T>(
    vararg val anns: NestedWithSameTypeArgument<T>
)

@NestedWithSameTypeArgument<A>(
    NestedWithSameTypeArgument()
)
expect fun explicitVsInfered()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann<A>
actual fun sameTypeParam() {}

@Ann<A>
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun differentTypeParam() {}

@Ann<A>
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun differentWithSameName() {}

@Ann<A>
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun nonNullvsNull() {}

@Ann<Ann<out A>>
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun differentVariance() {}

@Ann<Ann<A>>
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun varianceVsNoVariance() {}

@Ann<Ann<in A>>
actual fun sameVariance() {}

@Ann<Ann<Any>>
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun startProjection() {}

@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<A>(),
)
actual fun complexSame() {}

@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<A>(),
)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun complexDiffer() {}

@NestedWithSameTypeArgument<A>(
    NestedWithSameTypeArgument<A>()
)
actual fun explicitVsInfered() {}
