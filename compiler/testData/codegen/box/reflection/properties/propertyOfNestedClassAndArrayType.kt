// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KMutableProperty1

class A {
    class B(val result: String)

    var p: A.B? = null
    var q: Array<Array<A.B>>? = null
}

fun box(): String {
    val a = A()

    val aq = A::class.members.single { it.name == "q" } as KMutableProperty1<A, Array<Array<A.B>>>
    aq.set(a, arrayOf(arrayOf(A.B("array"))))
    if (a.q!![0][0].result != "array") return "Fail array"

    val ap = A::class.members.single { it.name == "p" } as KMutableProperty1<A, A.B>
    ap.set(a, A.B("OK"))
    return a.p!!.result
}
