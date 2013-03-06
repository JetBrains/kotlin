package test

public trait TwoSuperclassesInvariantAndCovariantInferMutability: Object {

    public trait Super1: Object {
        public fun foo(): List<List<String>>
    }

    public trait Super2: Object {
        public fun foo(): MutableList<MutableList<String>>
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): MutableList<MutableList<String>>
    }
}
