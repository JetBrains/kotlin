var a = A()
var b = A()

class A(var value: Int = 0)

fun prefix(inc: A.() -> A)  {
    ++a
}

fun postfix(inc: A.() -> A)  {
    b++
}

fun box(): String {
    prefix { ++value; this }

    if (a.value != 1) return "fail 1"

    postfix { value++; this }

    if (b.value != 1) return "fail 2"

    return "OK"
}