class A

fun A.foo() = this.(A::bar)("OK")

fun A.bar(x: String) = x

fun box() = A().foo()
