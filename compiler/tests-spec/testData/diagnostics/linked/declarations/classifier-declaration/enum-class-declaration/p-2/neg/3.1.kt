// !LANGUAGE: +NewInference
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: enum class cannot have any other base classes exept kotlin.Enum<E>
 */

// TESTCASE NUMBER: 1
sealed class Sealed()
enum class Case1 : <!CLASS_IN_SUPERTYPE_FOR_ENUM!>Sealed<!>()

// TESTCASE NUMBER: 2
open class Class2()
enum class Case2 : <!CLASS_IN_SUPERTYPE_FOR_ENUM!>Class2<!>()

// TESTCASE NUMBER: 3
class Case3 {
    open class Class3
    enum class EnumClass : <!CLASS_IN_SUPERTYPE_FOR_ENUM!>Class3<!>()
}

// TESTCASE NUMBER: 4
class Case4 {
    abstract class Class4
    enum class EnumClass : <!CLASS_IN_SUPERTYPE_FOR_ENUM!>Class4<!>()
}

// TESTCASE NUMBER: 5
enum class Case5 : <!CLASS_IN_SUPERTYPE_FOR_ENUM!>Any<!>()