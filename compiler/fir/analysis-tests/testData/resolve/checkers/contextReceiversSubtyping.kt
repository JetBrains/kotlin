// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

interface D: D1, D2 {}
interface D1 {}
interface D2 {}

<!SUBTYPING_BETWEEN_CONTEXT_RECEIVERS!>context(T, D2)<!>
fun <T> foo() where T: D1, T: D2 {}

interface Cov<out T> {}
class A {}
class B {}

// This sholdn't be an error since the type parameter is predetermined to be `A`,
// which causes that there can't be subtyping between the context receivers.
// However, the cheking is simplified because it is time-consuming process.
<!SUBTYPING_BETWEEN_CONTEXT_RECEIVERS!>context(Cov<T>, Cov<B>)<!>
fun <T: <!FINAL_UPPER_BOUND!>A<!>> foo() {}
