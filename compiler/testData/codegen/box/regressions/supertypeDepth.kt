// IGNORE_BACKEND_FIR: JVM_IR
class A : FirstOwner<Holder<A>>
class B : SecondOwner<Holder<B>>

interface FirstOwner<T : StubElement<*>> : SecondOwner<T>
interface SecondOwner<T : StubElement<*>>

interface StubElement<T>

interface Holder<T> : StubElement<T>

fun test(a: A?, b: B) {
    val c = a ?: b
}

fun box(): String {
    test(A(), B())
    return "OK"
}