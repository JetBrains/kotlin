interface I {
    fun foo(): String = "foo"

    fun bar(x: String = "default") = "bar:$x"
}

interface J : I

interface K : J

class A : I, J

class B : K, I

fun box(): String {
    val foo = A().foo()
    if (foo != "foo") return "fail1: $foo"

    val bar1 = A().bar()
    if (bar1 != "bar:default") return "fail2: $bar1"

    val bar2 = A().bar("q")
    if (bar2 != "bar:q") return "fail3: $bar2"

    val foo2 = B().foo()
    if (foo2 != "foo") return "fail4: $foo2"

    return "OK"
}