package test

public trait TypeParamOfClass: Object {

    public trait Super<T>: Object {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub<T>: Super<T> {
        override fun foo(): T
    }
}
