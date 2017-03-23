package test

public interface HalfSubstitutedTypeParameters {

    public interface TrickyList<X, E>: MutableList<E> {}

    public interface Super {
        public fun foo(): MutableList<String?>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): TrickyList<Int, String?>
    }
}
