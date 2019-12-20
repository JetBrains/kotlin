// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, type-checking-and-containment-checking-expressions, type-checking-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: the expression null is T? for any type T always evaluates to true
 */

fun box(): String {
    val x  = null
    if (x is Any? && x is Nothing? && x is String? && x is A?)
        if (!((x !is Any?) || (x !is Nothing?) || (x !is String?) || (x !is A?)))
        return "OK"
    return "NOK"
}

class A()