class A {
    class B(val result: String)

    var p: A.B? = null
    var q: Array<Array<A.B>>? = null
}

fun box(): String {
    val a = A()

    (A::q).set(a, array(array(A.B("array"))))
    if (a.q!![0][0].result != "array") return "Fail array"

    (A::p).set(a, A.B("OK"))
    return a.p!!.result
}
