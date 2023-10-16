annotation class A(vararg val x: Int)
annotation class B(val x: String, vararg val y: Int, val z: String)


@A
@B("x", z = "z")
fun foo() {}

@A(1)
@B("x", 1, z = "z")
fun bar() {}

@A(1, 2)
@B("x", 1, 2, z = "z")
fun baz() {}