// !LANGUAGE: -AllowTypeArgumentListLikeExpressions
// ISSUE: KT-8263

fun f(x: Int, y: Int, z: () -> Any, a: (Boolean, Boolean) -> Unit, b: (Any?) -> Unit) {
    a(x < -1, (-3) > 3)
    a(x < (-1), -3 > 3)
    a(x < (-1), (-3) > 3)
    a(x < y, -3 > 3)
    a(x < y, x > 2)

    b(x < if (y > (0)) y else 0)
    b(x < (if (y > (0)) y else 0))
    b(x < when (y > (0)) { else -> 0 })
    b(x < (when (y > (0)) { else -> 0 }))

    b(z() as Int < 15)
    b(z() as Int < y)
    b(z() is Int < true)
    b(z() !is Int < true)
    b(z() !is Int < true > false)
}