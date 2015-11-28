import test.*



fun box(): String {
    val a = A()
    if (a.foo<Any>() != a) return "failTypeCast 5"

    return "OK"
}