package test

public trait SubstitutedClassParameters {

    public trait Super1<T> {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Super2<E> {
        public fun foo(): E

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super1<String>, Super2<String> {
        override fun foo(): String
    }
}
