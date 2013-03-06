package test

public trait SubstitutedClassParameters: Object {

    public trait Super1<T>: Object {
        public fun foo(): T
    }

    public trait Super2<E>: Object {
        public fun foo(): E
    }

    public trait Sub: Super1<String>, Super2<String> {
        override fun foo(): String
    }
}