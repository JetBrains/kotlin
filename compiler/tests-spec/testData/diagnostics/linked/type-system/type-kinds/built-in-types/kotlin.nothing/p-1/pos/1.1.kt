// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNREACHABLE_CODE

// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: type-system, type-kinds, built-in-types, kotlin.nothing -> paragraph 1 -> sentence 1
 * SECONDARY LINKS: type-system, subtyping, subtyping-rules -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Check of Nothing type is a subtype of any types
 * HELPERS: checkType, functions
 */


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1
import checkSubtype
class NothingWrapper() {
    val data: Nothing = TODO()
}

class CustomClass() {}

fun case1() {
    val wrapper: NothingWrapper = NothingWrapper()
    checkSubtype<Any>(wrapper.data)
    checkSubtype<Any>(wrapper.data)

    checkSubtype<Any>(wrapper.data)
    checkSubtype<Function<Nothing>>(wrapper.data)
    checkSubtype<Int>(wrapper.data)
    checkSubtype<Short>(wrapper.data)
    checkSubtype<Byte>(wrapper.data)
    checkSubtype<Long>(wrapper.data)
    checkSubtype<kotlin.Array<Any>>(wrapper.data)
    checkSubtype<CustomClass>(wrapper.data)
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2
import checkSubtype
class NothingWrapper() {
    val data: Nothing = TODO()
}

class CustomClass() {}

fun case2(wrapper: NothingWrapper) {
    checkSubtype<MutableList<out Nothing>>(wrapper.data)
    checkSubtype<MutableList<in String>>(wrapper.data)
    checkSubtype<MutableList<out CustomClass>>(wrapper.data)
    checkSubtype<MutableList<in CustomClass>>(wrapper.data)
    checkSubtype<MutableList<Any?>>(wrapper.data)

    checkSubtype<String>(wrapper.data)
}