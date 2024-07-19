// LATEST_LV_DIFFERENCE
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
    <!MISSING_DEPENDENCY_CLASS!>produceBoxedConcrete<!>()
    <!MISSING_DEPENDENCY_CLASS!>produceBoxedGeneric<!>()

    <!MISSING_DEPENDENCY_CLASS!>consumeBoxedConcrete<!>(<!MISSING_DEPENDENCY_CLASS!>produceBoxedConcrete<!>())
    <!MISSING_DEPENDENCY_CLASS!>consumeBoxedGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!><!MISSING_DEPENDENCY_CLASS!>produceBoxedGeneric<!>()<!>)

    <!MISSING_DEPENDENCY_CLASS!>produceBoxedConcrete<!>().<!MISSING_DEPENDENCY_CLASS!>useBoxedConcreteAsExtensionReceiver<!>()
    <!MISSING_DEPENDENCY_CLASS!>produceBoxedGeneric<!>().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>useBoxedGenericAsExtensionReceiver<!>()

    <!MISSING_DEPENDENCY_CLASS!>withBoxedConcreteParameter<!> { arg -> }
    <!MISSING_DEPENDENCY_CLASS!>withBoxedGenericParameter<!> { arg -> }

    <!MISSING_DEPENDENCY_CLASS!>withBoxedConcreteParameter<!>(fun(arg) {})
    <!MISSING_DEPENDENCY_CLASS!>withBoxedGenericParameter<!>(fun(arg) {})

    <!MISSING_DEPENDENCY_CLASS!>withBoxedConcreteReceiver<!> {}
    <!MISSING_DEPENDENCY_CLASS!>withBoxedGenericReceiver<!> {}

    ::<!MISSING_DEPENDENCY_CLASS!>produceBoxedConcrete<!>
    ::<!MISSING_DEPENDENCY_CLASS!>produceBoxedGeneric<!>

    ::<!MISSING_DEPENDENCY_CLASS!>consumeBoxedConcrete<!>
    ::<!MISSING_DEPENDENCY_CLASS!>consumeBoxedGeneric<!>
}
