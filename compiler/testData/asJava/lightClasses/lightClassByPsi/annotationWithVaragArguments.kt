annotation class A(vararg val x: Int)
annotation class B(val x: String, vararg val y: Int, val z: String)
annotation class C(val a: A, val b: B)
annotation class D(vararg val x: String = ["a", "b"])
annotation class E(val d: D)

@A
@B(x = "x", z = "z")
@C(A(), B("x", z = "z"))
@D()
@E(d = D())
fun foo() {}

@A(1)
@B("x", 1, z = "z")
@C(A(1), B("x", 1, z = "z"))
fun bar() {}

@A(1, 2)
@B("x", 1, 2, z = "z")
@C(A(1, 2), B("x", 1, 2, z = "z"))
fun baz() {}
// DECLARATIONS_NO_LIGHT_ELEMENTS: A.class[x], B.class[x;y;z], C.class[a;b], D.class[x], E.class[d]
// LIGHT_ELEMENTS_NO_DECLARATION: A.class[x], B.class[x;y;z], C.class[a;b], D.class[x], E.class[d]