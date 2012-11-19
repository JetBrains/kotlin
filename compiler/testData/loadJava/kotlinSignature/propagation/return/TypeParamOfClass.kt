package test

public trait TypeParamOfClass: Object {

    public trait Super<T>: Object {
        public fun foo(): T
    }

    public trait Sub<T>: Super<T> {
        override fun foo(): T
    }
}
