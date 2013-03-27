package test

public trait OverrideWithErasedParameter: Object {

    public trait Super<T>: Object {
        public fun foo(p0: T?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub<T>: Super<T> {
        override fun foo(p0: T?)
    }
}