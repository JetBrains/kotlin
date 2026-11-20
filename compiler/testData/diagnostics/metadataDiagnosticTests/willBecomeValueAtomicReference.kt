// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.atomics.AtomicReference

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun test(ref: AtomicReference<Wrapper>, w: Wrapper, v: Wrapper) {
    ref.compareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>)
    <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w === v<!>
}
