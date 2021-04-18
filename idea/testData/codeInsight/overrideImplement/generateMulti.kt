// FIR_IDENTICAL
package something

interface Some<T> {
    fun someFoo()
    fun someOtherFoo() : Int
    fun someGenericFoo() : T
}

class SomeOther<S> : Some<S> {
    <caret>
}