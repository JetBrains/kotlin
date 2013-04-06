package test

public trait DeeplySubstitutedClassParameter2: Object {

    public trait Super<T>: Object {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Middle<E>: Super<E> {
    }

    public trait Sub: Middle<String> {
        override fun foo(): String
    }
}