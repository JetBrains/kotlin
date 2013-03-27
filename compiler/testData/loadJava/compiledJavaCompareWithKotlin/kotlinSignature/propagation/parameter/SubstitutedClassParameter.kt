package test

public trait SubstitutedClassParameter: Object {

    public trait Super<T>: Object {
        public fun foo(p0: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super<String> {
        override fun foo(p0: String)
    }
}