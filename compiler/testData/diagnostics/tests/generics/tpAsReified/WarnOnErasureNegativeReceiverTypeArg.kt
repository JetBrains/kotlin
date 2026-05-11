// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KTIJ-16774

class Holder<out T>(val value: T)
class NestedHolder<out W>(val wrapper: W)

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
inline fun <reified @kotlin.internal.WarnOnErasureUnconstrainedBy(<!WARN_ON_ERASURE_NEGATIVE_RECEIVER_TYPE_ARG!>-1<!>) T> Holder<*>.checkValueWithNegativeIndex(): Boolean = value is T

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
inline fun <reified @kotlin.internal.WarnOnErasureUnconstrainedBy(0, <!WARN_ON_ERASURE_NEGATIVE_RECEIVER_TYPE_ARG!>-1<!>) T> NestedHolder<Holder<*>>.checkInnerWithNegativeIndex(): Boolean = wrapper.value is T

/* GENERATED_FIR_TAGS: functionDeclaration, inline, isExpression, nullableType, reified, starProjection, stringLiteral,
typeParameter */
