package test

class KotlinClass {
    fun getKotlinClass() = KotlinClass()
}

class KotlinClass2 {
    val str = "HELLO"
}

fun useJavaClass() = CyclicDependencies().apply {
    useKotlinClass().let { useKotlinClass2(it) }
}.let { it.useKotlinClass2(it.useKotlinClass()) }
