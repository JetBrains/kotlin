// FIR_IDENTICAL
fun interface KRunnable {
    fun run()
}

fun foo0() {}
fun foo1(vararg xs: Int): Int = 1

fun use(r: KRunnable) {}

fun testSamConstructor() =
    KRunnable(::foo0)

// TODO should use an adapter function
fun testSamCosntructorOnAdapted() =
    KRunnable(::foo1)

fun testSamConversion() {
    use(::foo0)
}

// TODO should use an adapter function
fun testSamConversionOnAdapted() {
    use(::foo1)
}
