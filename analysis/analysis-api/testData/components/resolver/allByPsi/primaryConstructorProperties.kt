interface I
fun f(x: I) {
    class A(x: I) : I by x
    class B(val x: I): I by x
    class C : I by x
}
