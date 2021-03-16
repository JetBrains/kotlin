// FIR_IDENTICAL

interface A

open class B(<warning>i</warning>: Int, <warning>a</warning>: A)

class C() : B(3, object : A {})
