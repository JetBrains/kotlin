// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KTIJ-16774

inline fun <reified T> myCheck(x: Any): Boolean = x is T

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")
inline fun <reified @kotlin.internal.WarnOnErased T> myCheckedCheck(x: Any): Boolean = x is T

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

    // Should NOT warn: Int has no type parameters
    list.filterIsInstance<Int>()

    // Should warn: nested erasure
    list.filterIsInstance<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<List<String>><!>>()

    // Should NOT warn on custom reified function without @WarnOnErased
    myCheck<List<String>>(listOf("a"))

    // Should warn on custom function WITH @WarnOnErased
    myCheckedCheck<<!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>List<String><!>>(listOf("a"))

    // Should NOT warn: Array<String> is reified on JVM
    list.filterIsInstance<Array<String>>()
}

// Test suppression works
@Suppress("REIFIED_TYPE_UNSAFE_SUBSTITUTION")
fun testSuppressed(list: List<Any>) {
    list.filterIsInstance<List<String>>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, isExpression, nullableType, reified, starProjection, stringLiteral,
typeParameter */
