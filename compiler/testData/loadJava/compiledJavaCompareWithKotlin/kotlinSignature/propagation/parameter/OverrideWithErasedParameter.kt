package test

public trait OverrideWithErasedParameter {

    public trait Super<T> {
        public fun foo(p0: T?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub<T>: Super<T> {
        override fun foo(p0: T?)
    }
}
