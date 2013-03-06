package test

public trait DeeplySubstitutedClassParameter: Object {

    public trait Super<T>: Object {
        public fun foo(): T
    }

    public trait Middle<E>: Super<E> {
        override fun foo(): E
    }

    public trait Sub: Middle<String> {
        override fun foo(): String
    }
}