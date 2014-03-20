package test

public trait SubstitutedClassParameter: Object {

    public trait Super<T>: Object {
        public fun foo(t: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super<String> {
        override fun foo(t: String)
    }
}
