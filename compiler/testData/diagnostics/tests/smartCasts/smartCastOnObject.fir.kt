// ISSUE: KT-67374

object Some

inline fun <reified T> test_1(): T? {
    if (Some is T) return Some
    return null
}

fun test_2(): CharSequence? {
    if (<!USELESS_IS_CHECK!>Some is CharSequence<!>) return Some
    return null
}

typealias Other = Some

inline fun <reified T> test_3(): T? {
    if (Other is T) return Other
    return null
}

fun test_4(): CharSequence? {
    if (<!USELESS_IS_CHECK!>Other is CharSequence<!>) return Other
    return null
}
