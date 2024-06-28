// ISSUE: KT-64474, KT-66751
// MODULE: a
// FILE: a.kt

interface Concrete
interface Generic<T>

// MODULE: b(a)
// FILE: b.kt

interface Box<T>

fun produceBoxedConcrete(): Box<Concrete> = null!!
fun produceBoxedGeneric(): Box<Generic<*>> = null!!

fun consumeBoxedConcrete(arg: Box<Concrete>) {}
fun consumeBoxedGeneric(arg: Box<Generic<*>>) {}

fun Box<Concrete>.useBoxedConcreteAsExtensionReceiver() {}
fun Box<Generic<*>>.useBoxedGenericAsExtensionReceiver() {}

fun withBoxedConcreteParameter(arg: (Box<Concrete>) -> Unit) {}
fun withBoxedGenericParameter(arg: (Box<Generic<*>>) -> Unit) {}

fun withBoxedConcreteReceiver(arg: Box<Concrete>.() -> Unit) {}
fun withBoxedGenericReceiver(arg: Box<Generic<*>>.() -> Unit) {}

// MODULE: c(b)
// FILE: c.kt

fun test() {
    produceBoxedConcrete()
    produceBoxedGeneric()

    consumeBoxedConcrete(produceBoxedConcrete())
    consumeBoxedGeneric(produceBoxedGeneric())

    produceBoxedConcrete().useBoxedConcreteAsExtensionReceiver()
    produceBoxedGeneric().useBoxedGenericAsExtensionReceiver()

    withBoxedConcreteParameter { arg -> }
    withBoxedGenericParameter { arg -> }

    withBoxedConcreteParameter(fun(arg) {})
    withBoxedGenericParameter(fun(arg) {})

    withBoxedConcreteReceiver {}
    withBoxedGenericReceiver {}

    ::produceBoxedConcrete
    ::produceBoxedGeneric

    ::consumeBoxedConcrete
    ::consumeBoxedGeneric
}
