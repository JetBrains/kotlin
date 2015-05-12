fun foo() {
    interface T

    <selection>fun bar1<A, B, C>(a: A, b: B, c: C): A where B: A, C: B, C: T = c</selection>

    fun bar2<X, Y: X, Z>(x: X, y: Y, z: Z): X where Z: Y, Z: T = z

    fun bar3<X, Y, Z: Y>(x: X, y: Y, z: Z): X where Y: X, Z: T = z

    fun bar4<X, Y: X, Z: Y>(x: X, y: Y, z: Z): X where Z: T = z

    fun bar5<X, Y: X, Z: T>(x: X, y: Y, z: Z): X where Z: Y = z

    fun bar6<X, Z: Y, Y>(x: X, y: Y, z: Z): X where Y: X, Z: T = z

    fun bar7<X, Y: T, Z>(x: X, y: Y, z: Z): X where Y: X, Z: Y, Z: T = z
}