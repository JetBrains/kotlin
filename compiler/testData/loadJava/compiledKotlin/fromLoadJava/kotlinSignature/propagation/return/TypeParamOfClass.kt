package test

public interface TypeParamOfClass {

    public interface Super<T> {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub<T>: Super<T> {
        override fun foo(): T
    }
}
