package test

public trait TypeParamOfClass {

    public trait Super<T> {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub<T>: Super<T> {
        override fun foo(): T
    }
}
