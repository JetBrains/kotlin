// !LANGUAGE: +NewInference
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: type-system, type-kinds, built-in-types, kotlin.nothing -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Check of Nothing type is a subtype of any types
 * HELPERS: checkType
 */



class NothingWrapper() {
    val data: Nothing
}

class CustomClass

// TESTCASE NUMBER: 1
fun case1(wrapper: NothingWrapper) {
    checkSubtype<Any>(wrapper.data)
    checkSubtype<Function<Nothing>>(wrapper.data)
    checkSubtype<Int>(wrapper.data)
    checkSubtype<Short>(wrapper.data)
    checkSubtype<Byte>(wrapper.data)
    checkSubtype<Long>(wrapper.data)
    checkSubtype<kotlin.Array>(wrapper.data)
    checkSubtype<CustomClass>(wrapper.data)
}

// TESTCASE NUMBER: 2
fun case2(wrapper: NothingWrapper) {
    checkSubtype<MutableList<out Nothing>>(wrapper.data)
    checkSubtype<MutableList<in String>>(wrapper.data)
    checkSubtype<MutableList<Any?>>(wrapper.data)

    checkSubtype<String>(wrapper.data)

    checkSubtype<in CustomClass>(wrapper.data)
    checkSubtype<out CustomClass>(wrapper.data)
}