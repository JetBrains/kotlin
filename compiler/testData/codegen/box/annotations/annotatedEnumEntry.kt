// TARGET_BACKEND: JVM

// WITH_STDLIB
// KT-5665
// JVM_ABI_K1_K2_DIFF: KT-62470

@Retention(AnnotationRetention.RUNTIME)
annotation class First

@Retention(AnnotationRetention.RUNTIME)
annotation class Second(val value: String)

enum class E {
    @First
    E1 {
        fun foo() = "something"
    },

    @Second("OK")
    E2
}

fun box(): String {
    val e = E::class.java

    val e1 = e.getDeclaredField(E.E1.toString()).getAnnotations()
    if (e1.size != 1) return "Fail E1 size: ${e1.toList()}"
    if (e1[0].annotationClass.java != First::class.java) return "Fail E1: ${e1.toList()}"

    val e2 = e.getDeclaredField(E.E2.toString()).getAnnotations()
    if (e2.size != 1) return "Fail E2 size: ${e2.toList()}"
    if (e2[0].annotationClass.java != Second::class.java) return "Fail E2: ${e2.toList()}"

    return (e2[0] as Second).value
}
