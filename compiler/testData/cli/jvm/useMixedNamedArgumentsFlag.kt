fun foo(x: Int, y: Int, z: Int): Int = x + y + z

fun test() {
    foo(1, y = 2, 3)
}
