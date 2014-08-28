package test

public trait DeeplySubstitutedClassParameter2 {

    public trait Super<T> {
        public fun foo(t: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Middle<E>: Super<E> {
    }

    public trait Sub: Middle<String> {
        override fun foo(t: String)
    }
}
