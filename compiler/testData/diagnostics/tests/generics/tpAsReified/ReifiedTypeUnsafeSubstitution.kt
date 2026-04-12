// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KTIJ-16774

fun test(list: List<Any>) {
    // Should warn: List<String> is erased to List<*> at runtime
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>()

    // Should warn: Map<String, Int> is erased
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>Map<String, Int><!>>()

    // Should NOT warn: no type parameters to erase
    list.filterIsInstance<String>()

    // Should NOT warn: star projection already represents "don't know"
    list.filterIsInstance<List<*>>()

    // Should warn: Comparable<String> is erased
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>Comparable<String><!>>()

    // Should warn: nested erasure
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<List<String>><!>>()

    // Should NOT warn: Array<String> is reified on JVM
    list.filterIsInstance<Array<String>>()
}

// Test suppression works
@Suppress("REIFIED_TYPE_UNSAFE_SUBSTITUTION")
fun testSuppressed(list: List<Any>) {
    list.filterIsInstance<List<String>>()
}

// === Tests for receiver element type precision ===

sealed class MyResult<out R, out E>
class MySuccess<out R>(val value: R) : MyResult<R, Nothing>()
class MyFailure<out E>(val error: E) : MyResult<Nothing, E>()

fun testSealedHierarchy(list: List<MyResult<String, Exception>>) {
    // Should NOT warn: receiver element type MyResult<String, Exception> constrains MySuccess's R to String
    list.filterIsInstance<MySuccess<String>>()

    // Should still warn: receiver element type doesn't constrain List's type argument
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>()
}

fun testSameClassElementType(list: List<List<String>>) {
    // Should NOT warn: element type List<String> matches target exactly
    list.filterIsInstance<List<String>>()
}

fun testStarProjectedReceiver(list: List<*>) {
    // Should still warn: star projection loses element type info
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>()
}

open class MyBase<out T>(val value: T)
class MyDerived<out T>(value: T, val extra: String) : MyBase<T>(value)

fun testCovariantHierarchy(list: List<MyBase<String>>) {
    // Should NOT warn: MyBase<String> constrains MyDerived's T to String
    list.filterIsInstance<MyDerived<String>>()
}

fun testSequenceReceiver(seq: Sequence<MyResult<String, Exception>>) {
    // Should NOT warn: Sequence receiver also provides element type info
    seq.filterIsInstance<MySuccess<String>>()
}

// === Tests for explicit receiverTypeArg = 0 on non-collection receivers ===

class Holder<out T>(val value: T)

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
inline fun <reified @kotlin.internal.WarnOnErasureUnconstrainedBy(0) T> Holder<*>.checkValue(): Boolean = value is T

fun testNonCollectionTypedReceiver(holder: Holder<MyResult<String, Exception>>) {
    // Should NOT warn: Holder's type arg MyResult<String, Exception> constrains MySuccess's R to String
    holder.checkValue<MySuccess<String>>()

    // Should warn: Holder's type arg MyResult<String, Exception> doesn't constrain List's type arg
    holder.checkValue<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>()
}

fun testNonCollectionStarReceiver(holder: Holder<*>) {
    // Should warn: Holder<*> provides no type arg constraint
    holder.checkValue<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>MySuccess<String><!>>()
}

// === Tests for default receiverTypeArg (receiver type itself as constraint) ===

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
inline fun <reified @kotlin.internal.WarnOnErasureUnconstrainedBy T> MyBase<*>.checkSelf(): Boolean = this is T

fun testReceiverTypeConstraint(base: MyBase<String>) {
    // Should NOT warn: MyBase<String> constrains MyDerived's T to String through hierarchy
    base.checkSelf<MyDerived<String>>()

    // Should warn: MyBase<String> cannot constrain List's type argument
    base.checkSelf<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>()
}

fun testReceiverTypeStarConstraint(base: MyBase<*>) {
    // Should warn: MyBase<*> has star projection, cannot constrain MyDerived's T
    base.checkSelf<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>MyDerived<String><!>>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, isExpression, nullableType, reified, starProjection, stringLiteral,
typeParameter */
