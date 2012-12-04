package test

public trait TwoSuperclassesInvariantAndCovariantInferNullability: Object {

    public trait Super1: Object {
        public fun foo(): List<String?>
    }

    public trait Super2: Object {
        public fun foo(): MutableList<String>
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): MutableList<String>
    }
}
