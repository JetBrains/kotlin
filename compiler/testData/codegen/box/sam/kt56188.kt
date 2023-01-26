// IGNORE_BACKEND: JS

fun interface Supplier<SupplierTP> {
    fun get(): SupplierTP
}

fun <FooTP> foo(t: FooTP): Supplier<FooTP> {
    return Supplier<FooTP> { t } as Supplier<FooTP>
}

// Let's do some enterprise-style programming shall we
fun interface SupplierInvoker<SupplierInvokerTP1, SupplierInvokerTP2: Supplier<SupplierInvokerTP1>> {
    fun invokeSupplier(supplier: SupplierInvokerTP2): SupplierInvokerTP1
}

fun <BarTP1, BarTP2 : Supplier<BarTP1>> bar(): SupplierInvoker<BarTP1, BarTP2> {
    return SupplierInvoker<BarTP1, BarTP2> { supplier -> supplier.get() } as SupplierInvoker<BarTP1, BarTP2>
}

fun box(): String {
    val result1 = foo("OK").get()
    if (result1 != "OK") return result1

    val result2 = bar<String, Supplier<String>>().invokeSupplier { "OK" }
    if (result2 != "OK") return result2

    return "OK"
}
