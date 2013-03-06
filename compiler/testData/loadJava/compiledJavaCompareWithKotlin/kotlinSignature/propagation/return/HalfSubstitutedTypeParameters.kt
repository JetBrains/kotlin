package test

public trait HalfSubstitutedTypeParameters: Object {

    public trait TrickyList<X, E>: MutableList<E> {}

    public trait Super: Object {
        public fun foo(): MutableList<String?>
    }

    public trait Sub: Super {
        override fun foo(): TrickyList<Int, String?>
    }
}
