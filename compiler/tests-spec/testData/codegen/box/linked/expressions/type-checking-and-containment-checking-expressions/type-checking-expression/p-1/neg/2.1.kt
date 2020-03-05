// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, type-checking-and-containment-checking-expressions, type-checking-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: E is T: The type T must be runtime-available, otherwise it is a compiler error
 * EXCEPTION: compiletime
 */

fun box() {
    val x = A(1)

    val y = x is A<Long>
}

class A<T>(val a: T)
