package test

public interface InheritMutability {

    public interface Super {
        public fun foo(p: MutableList<String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p: MutableList<String>)
    }
}
