// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 2 -> sentence 5
 * PRIMARY LINKS: declarations, classifier-declaration, enum-class-declaration -> paragraph 4 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: enum class cannot have type parameters (intersection)
 */
// TESTCASE NUMBER: 1
enum class Case1<!TYPE_PARAMETERS_IN_ENUM!><T><!>(val x: T) where T : CharSequence, T : List<Int>

// TESTCASE NUMBER: 2
enum class Case2<!TYPE_PARAMETERS_IN_ENUM!><out T><!>(val x: T) where T : CharSequence, T : List<Int>

// TESTCASE NUMBER: 3
enum class Case3<!TYPE_PARAMETERS_IN_ENUM!><in T><!>(x: T) where T : CharSequence, T : List<Int>


// TESTCASE NUMBER: 4
class Case4<T> {
    enum class Case1<!TYPE_PARAMETERS_IN_ENUM!><T><!>(val x: T) where T : CharSequence, T : List<Int>

    enum class Case2<!TYPE_PARAMETERS_IN_ENUM!><out T><!>(val x: T) where T : CharSequence, T : List<Int>

    enum class Case3<!TYPE_PARAMETERS_IN_ENUM!><in T><!>(x: T) where T : CharSequence, T : List<Int>


}

