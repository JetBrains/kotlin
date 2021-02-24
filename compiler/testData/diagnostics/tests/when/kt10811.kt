// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, conditional-expression -> paragraph 4 -> sentence 1
 * type-inference, local-type-inference -> paragraph 8 -> sentence 1
 * type-inference, local-type-inference -> paragraph 2 -> sentence 1
 * type-system, subtyping, subtyping-rules -> paragraph 2 -> sentence 3
 * type-system, subtyping, subtyping-for-nullable-types -> paragraph 4 -> sentence 1
 * type-system, subtyping, subtyping-for-nullable-types -> paragraph 4 -> sentence 2
 */

interface Maybe<T>
class Some<T>(val value: T) : Maybe<T>
class None<T> : Maybe<T>

fun <T> none() : None<T> = TODO()

fun test1() : Maybe<String?> = if (true) none() else Some("")

fun test2() : Maybe<String?> = when {
    true -> none()
    else -> Some("")
}

fun test3() : Maybe<String?> = when {
    true -> none()
    else -> Some<String?>("")
}

fun test4() : Maybe<String?> {
    when ("") {
        "a" -> return none()
        else -> return Some<String?>("")
    }
}