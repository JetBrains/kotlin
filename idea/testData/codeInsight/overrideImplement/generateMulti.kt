package something

trait Some<T> {
    fun someFoo()
    fun someOtherFoo() : Int
    fun someGenericFoo() : T
}

class SomeOther<S> : Some<S> {
    <caret>
}