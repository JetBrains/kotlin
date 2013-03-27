package test

public trait DeeplySubstitutedClassParameter: Object {

    public trait Super<T>: Object {
        public fun foo(p0: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Middle<E>: Super<E> {
        override fun foo(p0: E)
    }

    public trait Sub: Middle<String> {
        override fun foo(p0: String)
    }
}