// DISABLE-ERRORS
fun foo() {
    <selection>(a + b*x.f(n - 1))</selection>
    (a + b)*x.f(n - 1)
    (a) + b*x.f(n - 1)
}