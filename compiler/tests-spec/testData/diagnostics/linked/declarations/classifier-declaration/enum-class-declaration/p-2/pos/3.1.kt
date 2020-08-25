// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 2 -> sentence 3
 * PRIMARY LINKS: declarations, classifier-declaration, enum-class-declaration -> paragraph 4 -> sentence 1
 * declarations, classifier-declaration, enum-class-declaration -> paragraph 5 -> sentence 1
 * declarations, classifier-declaration, enum-class-declaration -> paragraph 7 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: enum class implicitly inherits the built-in class kotlin.Enum<E>
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
enum class Case1

fun case1(case1: Case1) {
    checkSubtype<Enum<Case1>>(case1)
    val x: Case1 = Case1.valueOf("")
    val y: Array<Case1> = Case1.values()
}

// TESTCASE NUMBER: 2
fun case2() {
    Case2.valueOf("VAL1") checkType { check<Case2>() }
    Case2.values() checkType { check<Array<Case2>>() }
}

interface I2 {
    companion object {
        fun valueOf(value: String = "def"): String {
            println("Interface valueOf()")
            return value
        }

        fun values(): String {
            println("Interface values()")
            return ""
        }
    }
}


enum class Case2 : I2 {
    VAL1, VAL2;
}
// TESTCASE NUMBER: 3
fun case3() {
    Case3.valueOf("VAL1") checkType { check<Case3>() }
    Case3.values() checkType { check<Array<Case3>>() }
}
enum class Case3 {
    VAL1, VAL2;

    companion object {
        fun valueOf(value: String = "def"): String {
            println("Interface valueOf()")
            return value
        }

        fun values(): String {
            println("Interface values()")
            return ""
        }
    }
}
