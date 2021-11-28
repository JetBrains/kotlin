// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, type-checking-and-containment-checking-expressions, type-checking-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: E is T: The type T must be runtime-available, otherwise it is a compiler error
 * EXCEPTION: compiletime
 */

fun box() {
    val x = A(1)

    val y = x is A<Long>
}

class A<T>(val a: T)
