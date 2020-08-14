// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 2 -> sentence 5
 * PRIMARY LINKS: declarations, classifier-declaration, enum-class-declaration -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: enum class cannot have type parameters
 */
// TESTCASE NUMBER: 1
enum class Case1<!TYPE_PARAMETERS_IN_ENUM!><T><!>(val x: T)

// TESTCASE NUMBER: 2
enum class Case2<!TYPE_PARAMETERS_IN_ENUM!><out T><!>(val x: T)

// TESTCASE NUMBER: 3
enum class Case3<!TYPE_PARAMETERS_IN_ENUM!><in T><!>(private val x: T)

// TESTCASE NUMBER: 4
enum class Case4<!TYPE_PARAMETERS_IN_ENUM!><T : CharSequence><!>(val x: T)

// TESTCASE NUMBER: 5
enum class Case5<!TYPE_PARAMETERS_IN_ENUM!><out T : CharSequence><!>(val x: T)

// TESTCASE NUMBER: 6
enum class Case6<!TYPE_PARAMETERS_IN_ENUM!><in T : CharSequence><!>(private val x: T)

// TESTCASE NUMBER: 7
class Case7<T> {
    enum class Case1<!TYPE_PARAMETERS_IN_ENUM!><T><!>(val x: T)

    enum class Case2<!TYPE_PARAMETERS_IN_ENUM!><out T><!>(val x: T)

    enum class Case3<!TYPE_PARAMETERS_IN_ENUM!><in T><!>(x: T)

    enum class Case4<!TYPE_PARAMETERS_IN_ENUM!><T : CharSequence><!>(val x: T)

    enum class Case5<!TYPE_PARAMETERS_IN_ENUM!><out T : CharSequence><!>(val x: T)

    enum class Case6<!TYPE_PARAMETERS_IN_ENUM!><in T : CharSequence><!>(x: T)
}

// TESTCASE NUMBER: 8
class Case8 {
    enum class Case1<!TYPE_PARAMETERS_IN_ENUM!><R, T : R><!>(val x: T)

    enum class Case2<!TYPE_PARAMETERS_IN_ENUM!><R, out T : R><!>(val x: T)

    enum class Case3<!TYPE_PARAMETERS_IN_ENUM!><R, in T : R><!>(x: T)
}
