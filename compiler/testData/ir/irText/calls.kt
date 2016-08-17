fun foo(x: Int, y: Int) = x
fun bar(x: Int) = foo(x, 1)
fun qux(x: Int) = foo(foo(x, x), x)

fun Int.ext1() = this
fun Int.ext2(x: Int) = foo(this, x)
fun Int.ext3(x: Int) = foo(ext1(), x)