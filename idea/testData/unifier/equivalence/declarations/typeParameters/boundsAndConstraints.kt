fun foo() {
    abstract class T

    <selection>fun <A, B, C> bar1(a: A, b: B, c: C): A where B: A, C: B, C: T = c</selection>

    fun <X, Y: X, Z> bar2(x: X, y: Y, z: Z): X where Z: Y, Z: T = z

    fun <X, Y, Z: Y> bar3(x: X, y: Y, z: Z): X where Y: X, Z: T = z

    fun <X, Y: X, Z: Y> bar4(x: X, y: Y, z: Z): X where Z: T = z

    fun <X, Y: X, Z: T> bar5(x: X, y: Y, z: Z): X where Z: Y = z

    fun <X, Z: Y, Y> bar6(x: X, y: Y, z: Z): X where Y: X, Z: T = z

    fun <X, Y: T, Z> bar7(x: X, y: Y, z: Z): X where Y: X, Z: Y, Z: T = z
}