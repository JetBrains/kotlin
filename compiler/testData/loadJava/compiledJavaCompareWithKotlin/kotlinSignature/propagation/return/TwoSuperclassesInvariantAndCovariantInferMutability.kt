package test

public trait TwoSuperclassesInvariantAndCovariantInferMutability: Object {

    public trait Super1: Object {
        public fun foo(): List<List<String>>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Super2: Object {
        public fun foo(): MutableList<MutableList<String>>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): MutableList<MutableList<String>>
    }
}
