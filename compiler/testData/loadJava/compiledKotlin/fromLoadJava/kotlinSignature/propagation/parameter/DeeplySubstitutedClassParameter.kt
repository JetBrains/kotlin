package test

public trait DeeplySubstitutedClassParameter {

    public trait Super<T> {
        public fun foo(t: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Middle<E>: Super<E> {
        override fun foo(t: E)
    }

    public trait Sub: Middle<String> {
        override fun foo(t: String)
    }
}
