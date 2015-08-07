// KT-5665

annotation(retention = AnnotationRetention.RUNTIME) class First

annotation(retention = AnnotationRetention.RUNTIME) class Second(val value: String)

enum class E {
    @First
    E1 {
        fun foo() = "something"
    },

    @Second("OK")
    E2
}

fun box(): String {
    val e = javaClass<E>()

    val e1 = e.getDeclaredField(E.E1.toString()).getAnnotations()
    if (e1.size() != 1) return "Fail E1 size: ${e1.toList()}"
    if (e1[0].annotationType() != javaClass<First>()) return "Fail E1: ${e1.toList()}"

    val e2 = e.getDeclaredField(E.E2.toString()).getAnnotations()
    if (e2.size() != 1) return "Fail E2 size: ${e2.toList()}"
    if (e2[0].annotationType() != javaClass<Second>()) return "Fail E2: ${e2.toList()}"

    return (e2[0] as Second).value
}
