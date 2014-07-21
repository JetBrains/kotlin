package test

public trait HalfSubstitutedTypeParameters {

    public trait TrickyList<X, E>: MutableList<E> {}

    public trait Super {
        public fun foo(): MutableList<String?>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): TrickyList<Int, String?>
    }
}
