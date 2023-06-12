// FIR_IDENTICAL
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, subtyping, subtyping-rules -> paragraph 2 -> sentence 1
 * SECONDARY LINKS: type-system, subtyping -> paragraph 2 -> sentence 1
 * type-system, subtyping -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: type-system, subtyping, subtyping-rules -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: type T is subtype of Any and Noting is subtype of T
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1
class A

fun foo() : A = A()

interface AI{
    val ai0: String
        get() = ""
}

val AI.ai1: Int
    get() = 1


fun case1(a: A, ai: AI , nothing: Nothing) {

    checkSubtype<Any>(a)
    checkSubtype<A>(nothing)

    checkSubtype<Any>(foo())
    checkSubtype<A>(nothing)

    checkSubtype<Any>(ai)
    checkSubtype<AI>(nothing)

    checkSubtype<Any>(ai.ai0)
    checkSubtype<String>(nothing)

    checkSubtype<Any>(ai.ai1)
    checkSubtype<Int>(nothing)

    checkSubtype<Any>(nothing)
    checkSubtype<Nothing>(nothing)
}

// TESTCASE NUMBER: 2
fun case2( nothing: Nothing) {

    checkSubtype<Any>("a")
    checkSubtype<String>(nothing)
    checkSubtype<CharSequence>(nothing)
    checkSubtype<CharSequence>("")

    checkSubtype<Any>(1)
    checkSubtype<Int>(nothing)

    checkSubtype<Any>(1.0)
    checkSubtype<Double>(nothing)

    checkSubtype<Any>(true)
    checkSubtype<Boolean>(nothing)

    checkSubtype<Any>(Unit)
    checkSubtype<Unit>(nothing)

    checkSubtype<Any>(Exception())
    checkSubtype<Exception>(nothing)
}
// TESTCASE NUMBER: 3

class A3(val x : Int){

    class Nested{
        fun case3(nothing: Nothing) {
            checkSubtype<Any>(this)
            checkSubtype<A3.Nested>(this)
            checkSubtype<A3.Nested>(nothing)
        }
    }

    inner class AInner() {
        fun foo() = x
        fun case3(nothing: Nothing) {
            checkSubtype<Any>(this)
            checkSubtype<A3.AInner>(this)
            checkSubtype<A3.AInner>(nothing)
        }
    }


    companion object{
        private fun case3(nothing: Nothing) {
            checkSubtype<Any>(this)
            checkSubtype<A3.Companion>(this)
            checkSubtype<A3.Companion>(nothing)
        }
    }
}

// TESTCASE NUMBER: 4
class A4(val x: Int) {

    interface AN

    inner class AInner() {
        fun foo() = x
    }

    companion object {}
}

fun case4(a: A4, an: A4.AN, nothing: Nothing) {

    checkSubtype<Any>(A4.Companion)
    checkSubtype<A4.Companion>(nothing)

    checkSubtype<Any>(a.AInner())
    checkSubtype<A4.AInner>(nothing)

    checkSubtype<Any>(an)
    checkSubtype<A4.AN>(nothing)
}
