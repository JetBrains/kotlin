// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!><T>

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>
private object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>FakeA<!> {
    class A
}

<!CONFLICTING_OVERLOADS!>@Ann<A>
expect fun sameTypeParam()<!>

<!CONFLICTING_OVERLOADS!>@Ann<B>
expect fun differentTypeParam()<!>

<!CONFLICTING_OVERLOADS!>@Ann<<!INVISIBLE_REFERENCE!>FakeA<!>.<!INVISIBLE_REFERENCE!>A<!>>
expect fun differentWithSameName()<!>

<!CONFLICTING_OVERLOADS!>@Ann<A?>
expect fun nonNullvsNull()<!>

<!CONFLICTING_OVERLOADS!>@Ann<Ann<in A>>
expect fun differentVariance()<!>

<!CONFLICTING_OVERLOADS!>@Ann<Ann<in A>>
expect fun varianceVsNoVariance()<!>

<!CONFLICTING_OVERLOADS!>@Ann<Ann<in A>>
expect fun sameVariance()<!>

<!CONFLICTING_OVERLOADS!>@Ann<Ann<*>>
expect fun startProjection()<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ComplexNested<!><T>(
    vararg val anns: ComplexNested<*>,
)

<!CONFLICTING_OVERLOADS!>@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<A>(),
)
expect fun complexSame()<!>

<!CONFLICTING_OVERLOADS!>@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<B>(),
)
expect fun complexDiffer()<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>NestedWithSameTypeArgument<!><T>(
    vararg val anns: NestedWithSameTypeArgument<T>
)

<!CONFLICTING_OVERLOADS!>@NestedWithSameTypeArgument<A>(
    NestedWithSameTypeArgument()
)
expect fun explicitVsInfered()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann<A>
actual fun sameTypeParam() {}

@Ann<A>
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>differentTypeParam<!>() {}

@Ann<A>
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>differentWithSameName<!>() {}

@Ann<A>
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>nonNullvsNull<!>() {}

@Ann<Ann<out A>>
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>differentVariance<!>() {}

@Ann<Ann<A>>
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>varianceVsNoVariance<!>() {}

@Ann<Ann<in A>>
actual fun sameVariance() {}

@Ann<Ann<Any>>
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>startProjection<!>() {}

@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<A>(),
)
actual fun complexSame() {}

@ComplexNested<A>(
    ComplexNested<A>(),
    ComplexNested<A>(),
)
actual fun complexDiffer() {}

@NestedWithSameTypeArgument<A>(
    NestedWithSameTypeArgument<A>()
)
actual fun explicitVsInfered() {}
