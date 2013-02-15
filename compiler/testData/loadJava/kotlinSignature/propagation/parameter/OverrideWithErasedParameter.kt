package test

public trait OverrideWithErasedParameter: Object {

    public trait Super<T>: Object {
        public fun foo(p0: T?)
    }

    public trait Sub<T>: Super<T> {
        override fun foo(p0: T?)
    }
}