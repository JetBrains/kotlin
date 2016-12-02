// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE

open class A {
    protected fun test() {}

    protected val z: String = "1"

    public var zVar: String = "1"
        protected set(value) {}

    inline fun call() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>test<!>()
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>z<!>
        zVar
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>zVar<!> = "123"
    }

    internal inline fun callFromInternal() {
        test()
        zVar
        zVar = "123"
    }

    @PublishedApi
    internal inline fun callFromPublished() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>test<!>()
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>z<!>
        zVar
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>zVar<!> = "123"
    }
}

class B : A() {
    inline fun testB() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE!>test<!>()
    }
}


internal class AInternal {
    protected fun test() {}

    protected val z: String = "1"

    public var zVar: String = "1"
        protected set(value) {}


    inline fun call() {
        test()
    }

    @PublishedApi
    internal inline fun call2() {
        test()
    }
}