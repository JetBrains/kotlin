// !LANGUAGE: -AllowTypeArgumentListLikeExpressions
// ISSUE: KT-8263

fun f(x: Int, y: Int, z: () -> Any, a: (Boolean, Boolean) -> Unit, b: (Any?) -> Unit) {
    a(x < -1, (-3) > 3)
    a(x < (-1), -3 > 3)
    a(x <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> (-1), (-3) > 3)
    a(x < y, -3 > 3)
    a(x <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> y, x > 2)

    b(x < if (y > (0)) y else 0)
    b(x <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> (if (y > (0)) y else 0))
    b(x < when (y > (0)) { else -> 0 })
    b(x <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> (when (y > (0)) { else -> 0 }))

    b(z() as Int <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> 15)
    b(z() as Int <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> y)
    b(z() is Int <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> true)
    b(z() !is Int <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> true)
    b(z() !is Int <!TYPE_ARGUMENT_LIST_LIKE_EXPRESSION!><<!> true > false)
}
