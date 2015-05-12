package test

public interface OverrideWithErasedParameter {

    public interface Super<T> {
        public fun foo(p0: T?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub<T>: Super<T> {
        override fun foo(p0: T?)
    }
}
