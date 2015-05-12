package test

public interface TwoSuperclassesInvariantAndCovariantInferMutability {

    public interface Super1 {
        public fun foo(): List<List<String>>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Super2 {
        public fun foo(): MutableList<MutableList<String>>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super1, Super2 {
        override fun foo(): MutableList<MutableList<String>>
    }
}
