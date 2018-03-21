package aaa.bbb

class A {}

fun foo(x: Int, a: A): Int {
    var y = x + 2
    if (y > 2) {
        return 10
    } else {
        y += 10
    }
    return y
}

fun bar(): Int {
    val x = 10
    val y = foo(x)
    return y + 10
}

//fun foo(x: Int, y: Int) = x + y
