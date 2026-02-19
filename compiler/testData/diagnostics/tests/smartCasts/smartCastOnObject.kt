// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67374

object Some

inline fun <reified T> test_1(): T? {
    if (Some is T) return <!DEBUG_INFO_SMARTCAST!>Some<!>
    return null
}

fun test_2(): CharSequence? {
    if (Some is <!INCOMPATIBLE_TYPES!>CharSequence<!>) return <!DEBUG_INFO_SMARTCAST!>Some<!>
    return null
}

typealias Other = Some

inline fun <reified T> test_3(): T? {
    if (Other is T) return <!TYPE_MISMATCH!>Other<!>
    return null
}

fun test_4(): CharSequence? {
    if (Other is <!INCOMPATIBLE_TYPES!>CharSequence<!>) return <!TYPE_MISMATCH!>Other<!>
    return null
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, inline, intersectionType, isExpression, nullableType,
objectDeclaration, reified, smartcast, typeAliasDeclaration, typeParameter */
